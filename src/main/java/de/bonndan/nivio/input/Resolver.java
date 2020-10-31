package de.bonndan.nivio.input;

import de.bonndan.nivio.input.dto.LandscapeDescription;
import de.bonndan.nivio.model.Landscape;

abstract class Resolver {

    protected final ProcessLog processLog;

    protected Resolver(ProcessLog processLog) {
        this.processLog = processLog;
    }

    public abstract void process(LandscapeDescription input, Landscape landscape);
}
