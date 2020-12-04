
package de.bonndan.nivio.input;

import de.bonndan.nivio.ProcessingErrorEvent;
import de.bonndan.nivio.ProcessingException;
import de.bonndan.nivio.ProcessingFinishedEvent;
import de.bonndan.nivio.assessment.kpi.KPIFactory;
import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.input.dto.ItemDescription;
import de.bonndan.nivio.model.*;
import de.bonndan.nivio.output.icons.IconService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.*;

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
    private final ApplicationEventPublisher eventPublisher;
    private final IconService iconService;

    public Indexer(LandscapeRepository landscapeRepository,
                   InputFormatHandlerFactory formatFactory,
                   ApplicationEventPublisher eventPublisher,
                   IconService iconService
    ) {
        this.landscapeRepo = landscapeRepository;
        this.formatFactory = formatFactory;
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

        Map<ItemDescription, List<String>> templatesAndTargets = new HashMap<>();
        // read all input sources
        new SourceReferencesResolver(formatFactory, logger).resolve(input, templatesAndTargets);

        // apply template values to the items
        new TemplateResolver().processTemplates(input, templatesAndTargets);

        // read special labels on items and assign the values to fields
        new LabelToFieldProcessor(logger).process(input, landscape);

        // mask any label containing secrets
        new SecureLabelsProcessor().process(input);

        // create relation targets on the fly if the landscape is configured "greedy"
        new InstantItemResolver(logger).processTargets(input);

        // find items for relation endpoints (which can be queries, identifiers...)
        new RelationEndpointResolver(logger).processRelations(input);

        // add any missing groups
        new GroupResolver(logger).process(input, landscape);

        // compare landscape against input, add and remove items
        new DiffResolver(logger).process(input, landscape);

        // execute group "contains" queries
        new GroupQueryResolver(logger).process(input, landscape);

        // try to find "magic" relations by examining item labels for keywords
        new MagicLabelRelations(logger).process(input, landscape);

        // create relations between items
        new ItemRelationResolver(logger).process(input, landscape);

        // ensures that item have a resolved icon in the api
        new AppearanceResolver(logger, iconService).process(input, landscape);

        // this step must be final or very late to include all item modifications
        landscape.getItems().indexForSearch();
    }

}