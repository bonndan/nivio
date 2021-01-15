
package de.bonndan.nivio.input;

import de.bonndan.nivio.assessment.kpi.KPIFactory;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.external.LinkHandlerFactory;
import de.bonndan.nivio.model.*;
import de.bonndan.nivio.output.icons.IconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * This component is a wrapper around all the steps to examine and index an landscape input dto.
 *
 *
 */
@Component
public class Indexer {

    private static final Logger _logger = LoggerFactory.getLogger(Indexer.class);

    private final LandscapeRepository landscapeRepo;
    private final InputFormatHandlerFactory formatFactory;
    private final LinkHandlerFactory linkHandlerFactory;
    private final ApplicationEventPublisher eventPublisher;
    private final IconService iconService;

    public Indexer(LandscapeRepository landscapeRepository,
                   InputFormatHandlerFactory formatFactory,
                   LinkHandlerFactory linkHandlerFactory,
                   ApplicationEventPublisher eventPublisher,
                   IconService iconService
    ) {
        this.landscapeRepo = landscapeRepository;
        this.formatFactory = formatFactory;
        this.linkHandlerFactory = linkHandlerFactory;
        this.eventPublisher = eventPublisher;
        this.iconService = iconService;
    }

    /**
     * Indexes the given input and creates a landscape or updates an existing one.
     *
     * @param input dto
     * @return the log of the operation
     */
    public ProcessLog index(final LandscapeDescription input) {

        ProcessLog logger = new ProcessLog(_logger);

        Landscape landscape = landscapeRepo.findDistinctByIdentifier(input.getIdentifier()).orElseGet(() -> {
            logger.info("Creating new landscape " + input.getIdentifier());
            Landscape landscape1 = LandscapeFactory.create(input);
            landscapeRepo.save(landscape1);
            return landscape1;
        });
        LandscapeFactory.assignAll(input, landscape);
        logger.setLandscape(landscape);
        if (landscape.getLog() == null) {
            landscape.setProcessLog(logger);
        }

        try {
            runResolvers(input, landscape);
            landscapeRepo.save(landscape);
        } catch (ProcessingException e) {
            final String msg = "Error while reindexing landscape " + input.getIdentifier();
            logger.warn(msg, e);
            eventPublisher.publishEvent(new ProcessingErrorEvent(this, e));
        }

        eventPublisher.publishEvent(new ProcessingFinishedEvent(input, landscape));
        logger.info("Reindexed landscape " + input.getIdentifier());
        landscape.setProcessLog(logger);
        return logger;
    }

    private void runResolvers(LandscapeDescription input, Landscape landscape) {

        ProcessLog logger = landscape.getLog();

        //initialize KPIs
        KPIFactory kpiFactory = new KPIFactory();
        landscape.setKpis(kpiFactory.getConfiguredKPIs(input.getConfig().getKPIs()));

        // read all input sources
        new SourceReferencesResolver(formatFactory, logger).resolve(input);

        // apply template values to items
        new TemplateResolver(logger).resolve(input);

        // resolve links on components to gather more data.
        new LinksResolver(logger, linkHandlerFactory).resolve(input);

        // mask any label containing secrets
        new SecureLabelsResolver(logger).resolve(input);

        // create relation targets on the fly if the landscape is configured "greedy"
        new InstantItemResolver(logger).resolve(input);

        // read special labels on items and assign the values to fields
        new LabelToFieldResolver(logger).resolve(input);

        // find items for relation endpoints (which can be queries, identifiers...)
        // KEEP here (must run late after other resolvers)
        new RelationEndpointResolver(logger).resolve(input);

        // add any missing groups
        new GroupProcessor(logger).process(input, landscape);

        // compare landscape against input, add and remove items
        new DiffProcessor(logger).process(input, landscape);

        // execute group "contains" queries
        new GroupQueryProcessor(logger).process(input, landscape);

        // try to find "magic" relations by examining item labels for keywords
        new MagicLabelRelationProcessor(logger).process(input, landscape);

        // create relations between items
        new ItemRelationProcessor(logger).process(input, landscape);

        // ensures that item have a resolved icon in the api
        new AppearanceProcessor(logger, iconService).process(input, landscape);

        // this step must be final or very late to include all item modifications
        landscape.getItems().indexForSearch();
    }

}