/* *********************************************************************** *
 * project: org.matsim.*
 * MyControlerListener.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 *
 */

package org.matsim.optDRT;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

/**
 * @author ikaddoura
 */
public class OptDrtControlerListener implements IterationStartsListener, IterationEndsListener {

    private static final Logger log = LogManager.getLogger(OptDrtControlerListener.class);

    private final MultiModeOptDrtConfigGroup multiModeOptDrtCfg;
    
    private final OptDrtConfigGroup optDrtConfigGroup;

    private final OptDrtFareStrategy optDrtFareStrategy;

    private final OptDrtFleetStrategy optDrtFleetStrategy;

    private final OptDrtServiceAreaStrategy optDrtServiceAreaStrategy;

    private final Scenario scenario;

    public OptDrtControlerListener(MultiModeOptDrtConfigGroup multiModeOptDrtCfg, OptDrtConfigGroup optDrtConfigGroup, OptDrtFareStrategy optDrtFareStrategy,
            OptDrtFleetStrategy optDrtFleetStrategy, OptDrtServiceAreaStrategy optDrtServiceAreaStrategy,
            Scenario scenario) {
    	this.multiModeOptDrtCfg = multiModeOptDrtCfg;
    	this.optDrtConfigGroup = optDrtConfigGroup;
        this.optDrtFareStrategy = optDrtFareStrategy;
        this.optDrtFleetStrategy = optDrtFleetStrategy;
        this.optDrtServiceAreaStrategy = optDrtServiceAreaStrategy;
        this.scenario = scenario;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        if (optDrtConfigGroup.getWriteInfoInterval() != 0
                && iterationEndsEvent.getIteration() % optDrtConfigGroup.getWriteInfoInterval() == 0.) {

            this.optDrtFareStrategy.writeInfo( iterationEndsEvent.getIteration() );
            this.optDrtFleetStrategy.writeInfo( iterationEndsEvent.getIteration() );
            this.optDrtServiceAreaStrategy.writeInfo( iterationEndsEvent.getIteration() );
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent iterationStartsEvent) {
        // first run update methods which use data from the previous iteration
        if (multiModeOptDrtCfg.getUpdateInterval() != 0
                && iterationStartsEvent.getIteration() != this.scenario.getConfig().controler().getLastIteration()
                && iterationStartsEvent.getIteration() <= optDrtConfigGroup.getUpdateEndFractionIteration() * this.scenario.getConfig().controler().getLastIteration() + 1
                && ( iterationStartsEvent.getIteration() % multiModeOptDrtCfg.getUpdateInterval() == 1. || multiModeOptDrtCfg.getUpdateInterval() == 1 ) ) {

            log.info("Iteration " + iterationStartsEvent.getIteration() + ". Applying DRT strategies...");

            this.optDrtFareStrategy.updateFares( iterationStartsEvent.getIteration() );
            this.optDrtFleetStrategy.updateFleet( iterationStartsEvent.getIteration() );
            this.optDrtServiceAreaStrategy.updateServiceArea( iterationStartsEvent.getIteration() );

            log.info("Iteration " + iterationStartsEvent.getIteration() + ". Applying DRT strategies... Done.");

        }
        // then delete data from previous iteration to clean up for this iteration
        this.optDrtFareStrategy.resetDataForThisIteration( iterationStartsEvent.getIteration() );
        this.optDrtFleetStrategy.resetDataForThisIteration( iterationStartsEvent.getIteration() );
        this.optDrtServiceAreaStrategy.resetDataForThisIteration( iterationStartsEvent.getIteration() );
    }
}
