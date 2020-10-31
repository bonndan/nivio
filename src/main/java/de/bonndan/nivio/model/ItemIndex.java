package de.bonndan.nivio.model;

import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.attribute.support.SimpleFunction;
import com.googlecode.cqengine.query.parser.sql.SQLParser;
import com.googlecode.cqengine.resultset.ResultSet;
import de.bonndan.nivio.input.dto.ItemDescription;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.facet.FacetResult;
import org.apache.lucene.facet.Facets;
import org.apache.lucene.facet.FacetsCollector;
import org.apache.lucene.facet.FacetsConfig;
import org.apache.lucene.facet.taxonomy.FastTaxonomyFacetCounts;
import org.apache.lucene.facet.taxonomy.TaxonomyWriter;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyReader;
import org.apache.lucene.facet.taxonomy.directory.DirectoryTaxonomyWriter;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.googlecode.cqengine.query.QueryFactory.attribute;
import static de.bonndan.nivio.model.Item.IDENTIFIER_VALIDATION;
import static de.bonndan.nivio.model.SearchDocumentFactory.*;

/**
 * A queryable index on all landscape items.
 */
public class ItemIndex {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemIndex.class);
    private static final String CQE_FIELD_FQI = "fqi";

    /**
     * The {@link com.googlecode.cqengine.query.QueryFactory#attribute(String, SimpleFunction)})} relies on a method
     * {@link net.jodah.typetools.TypeResolver#resolveRawArguments(Type, Class)}, which in Java 13 is not able to retrieve
     * information about the generic types, if a lambda or anonymous method reference is provided. By providing an anonymous
     * class of the {@link SimpleFunction}, the generic types can be resolved without running into exceptions.
     */
    @SuppressWarnings({"Convert2Lambda"})
    private static final Attribute<Item, String> CQE_ATTR_FQI = attribute("fqi", new SimpleFunction<>() {
        @Override
        public String apply(Item item) {
            return item.getFullyQualifiedIdentifier().toString();
        }
    });

    /**
     * See {@link #CQE_ATTR_FQI}
     */
    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
    private static final Attribute<Item, String> CQE_ATTR_IDENTIFIER = attribute("identifier", new SimpleFunction<>() {
        @Override
        public String apply(Item item) {
            return item.getIdentifier();
        }
    });

    /**
     * See {@link #CQE_ATTR_FQI}
     */
    @SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
    private static final Attribute<Item, String> CQE_ATTR_NAME = attribute("name", new SimpleFunction<>() {
        @Override
        public String apply(Item item) {
            return item.getName();
        }
    });

    private final SQLParser<Item> parser;
    private final Directory searchIndex;
    private final Directory taxoIndex;

    IndexedCollection<Item> index = new ConcurrentIndexedCollection<>();

    /**
     * Creates a new empty index.
     */
    public ItemIndex() {

        //init cq engine
        parser = SQLParser.forPojoWithAttributes(Item.class,
                Map.of(
                        CQE_FIELD_FQI, CQE_ATTR_FQI,
                        "identifier", CQE_ATTR_IDENTIFIER,
                        "name", CQE_ATTR_NAME)
        );

        //init lucene
        searchIndex = new RAMDirectory();
        taxoIndex = new RAMDirectory();
    }

    public ItemIndex(Set<Item> items) {
        this();
        setItems(items);
    }

    public Stream<Item> itemStream() {
        return index.stream();
    }

    public void setItems(Set<Item> items) {
        index = new ConcurrentIndexedCollection<>();
        index.addAll(items);
    }

    /**
     * Returns all items matching the given term.
     *
     * @param term "*" as wildcard for all | {@link FullyQualifiedIdentifier} string pathes | identifier
     * @return all matching items.
     */
    public Collection<Item> query(String term) {
        if ("*".equals(term)) {
            return all();
        }

        if (term.contains("/")) {
            return findAll(ItemMatcher.forTarget(term));
        }

        //single word compared against identifier
        String query = term.matches(IDENTIFIER_VALIDATION) ? selectByIdentifierOrName(term) : "SELECT * FROM items WHERE " + term;
        return cqnQueryOnIndex(query);
    }

    /**
     * Returns a select query.
     *
     * @param term equals identifier or name
     * @return query string
     */
    public String selectByIdentifierOrName(String term) {
        return "SELECT * FROM items WHERE (identifier = '" + term + "' OR name = '" + term + "')";
    }

    public Set<Item> all() {
        return index;
    }

    /**
     * Ensures that the given item has a sibling in the list, returns the item from the list.
     *
     * @param itemDescription item to search for
     * @return the sibling from the list
     */
    public Item pick(final ItemDescription itemDescription) {
        return pick(itemDescription.getIdentifier(), itemDescription.getGroup());
    }

    /**
     * Makes sure the sibling of the item is returned or throws an exception.
     *
     * @param identifier identifier
     * @param group      the group to search in
     * @return the sibling with the given identifier
     */
    public Item pick(final String identifier, String group) {
        if (StringUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException("Identifier to pick is empty");
        }

        return find(identifier, group).orElseThrow(() ->
                new RuntimeException("Element '" + identifier + "' not found  in collection.")
        );
    }

    /**
     * Returns a the item from the list or null. Uses the matching criteria of {@link FullyQualifiedIdentifier}
     *
     * @param identifier the identifier
     * @return the item or null
     */
    public Optional<Item> find(String identifier, String group) {
        if (StringUtils.isEmpty(identifier)) {
            throw new IllegalArgumentException("Identifier to find is empty");
        }

        List<Item> found = findAll(identifier, group);

        if (found.size() > 1) {
            throw new RuntimeException("Ambiguous result for " + group + "/" + identifier + ": " + found + " in collection ");
        }

        return Optional.ofNullable((found.size() == 1) ? found.get(0) : null);
    }

    /**
     * Returns a the item from the list or null. Uses the matching criteria of {@link ItemMatcher}
     *
     * @param itemMatcher the identifier
     * @return the or null
     */
    public Optional<Item> find(ItemMatcher itemMatcher) {
        List<Item> found = findAll(itemMatcher);

        if (found.size() > 1) {
            throw new RuntimeException("Ambiguous result for " + itemMatcher + ": " + found + " in collection.");
        }

        return Optional.ofNullable((found.size() == 1) ? found.get(0) : null);
    }

    /**
     * Creates a search index based in a snapshot of current items state (later modifications won't be shown).
     *
     * @return number of indexed items
     */
    public int indexForSearch() {
        int indexed = 0;
        try {
            FacetsConfig config = SearchDocumentFactory.getConfig();
            TaxonomyWriter taxoWriter = new DirectoryTaxonomyWriter(taxoIndex, IndexWriterConfig.OpenMode.CREATE);
            IndexWriter writer = new IndexWriter(searchIndex, new IndexWriterConfig(new StandardAnalyzer()));
            writer.deleteAll();
            for (Item item : index) {

                Document doc = from(item);
                writer.addDocument(config.build(taxoWriter, doc));
                indexed++;
            }
            IOUtils.close(writer, taxoWriter); //, searchIndex, taxoIndex);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update search index", e);
        }

        return indexed;
    }

    public Set<Item> search(String queryString) {
        try {
            return documentSearch(queryString).stream()
                    .map(doc ->
                            //TODO this is ineffective, there must be a way (index?) to obtain the item directly
                            cqnQueryOnIndex("SELECT * FROM items WHERE " + CQE_FIELD_FQI + " = '" + doc.get(LUCENE_FIELD_FQI) + "'").stream().findFirst().orElse(null)
                    )
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Failed to execute search for " + queryString);
        }
    }

    /**
     * Returns the facets for the given query.
     *
     * @return top 10 facets
     */
    public List<FacetResult> facets() {
        try {
            DirectoryReader ireader = DirectoryReader.open(searchIndex);
            IndexSearcher searcher = new IndexSearcher(ireader);

            DirectoryTaxonomyReader taxoReader = new DirectoryTaxonomyReader(taxoIndex);
            FacetsCollector fc = new FacetsCollector();
            FacetsConfig config = getConfig();
            FacetsCollector.search(searcher, new MatchAllDocsQuery(), 10, fc);

            Facets facets = new FastTaxonomyFacetCounts(taxoReader, config, fc);
            ireader.close();
            return facets.getAllDims(10);
        } catch (IOException e) {
            LOGGER.warn("Unable to get the facets for the given query error: ", e);
        }

        return null;
    }

    private List<Document> documentSearch(String queryString) throws IOException, ParseException {

        DirectoryReader ireader = DirectoryReader.open(searchIndex);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        // Parse a simple query that searches for "text":
        QueryParser parser = new MultiFieldQueryParser(new String[]{LUCENE_FIELD_NAME, LUCENE_FIELD_DESCRIPTION}, new StandardAnalyzer());
        parser.setAllowLeadingWildcard(true);
        Query query = parser.parse(queryString);
        ScoreDoc[] hits = isearcher.search(query, 10).scoreDocs;

        List<Document> documents = new ArrayList<>();
        // Iterate through the results:
        for (ScoreDoc hit : hits) {
            Document hitDoc = isearcher.doc(hit.doc);
            documents.add(hitDoc);
        }
        ireader.close();

        return documents;
    }


    public List<Item> cqnQueryOnIndex(String condition) {


        ResultSet<Item> results = parser.retrieve(index, condition);
        return results.stream().collect(Collectors.toList());
    }

    private List<Item> findAll(final String identifier, final String group) {
        return findAll(ItemMatcher.build(null, group, identifier));
    }

    private List<Item> findAll(ItemMatcher itemMatcher) {
        return itemStream()
                .filter(item -> itemMatcher.isSimilarTo(item.getFullyQualifiedIdentifier()))
                .collect(Collectors.toList());
    }
}
