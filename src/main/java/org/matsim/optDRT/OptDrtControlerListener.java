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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningUtils;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach;

import com.google.inject.Inject;


/**
 *
 * @author ikaddoura
 *
 */

public class OptDrtControlerListener implements StartupListener, IterationEndsListener, IterationStartsListener {

    private static final Logger log = Logger.getLogger(OptDrtControlerListener.class);

    @Inject
    private OptDrtConfigGroup optDrtConfigGroup;

    @Inject
    private OptDrtFareStrategy optDrtFareStrategy;

    @Inject
    private OptDrtFleetStrategy optDrtFleetStrategy;

    @Inject
    private OptDrtServiceAreaStrategy optDrtServiceAreaStrategy;

    @Inject
    private Scenario scenario;

    @Inject
    private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies;

    @Inject
    private StrategyManager strategyManager;

    private int nextDisableInnovativeStrategiesIteration = -1;
    private int nextEnableInnovativeStrategiesIteration = -1;

    @Override
    public void notifyStartup(StartupEvent event) {
        log.info("optDrt settings: " + optDrtConfigGroup.toString());

        if (!optDrtConfigGroup.getOptDrtMode().equals("drt")) {
            throw new RuntimeException("Currently, a mode different from 'drt' is not allowed."
                    + " Only works for a single mode specified in OptDrtConfigGroup. At some point we might think about a modal binding."
                    + " Aborting... ");
        }

        if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled) {
            if (optDrtConfigGroup.getInputShapeFileForServiceAreaAdjustment() == null || optDrtConfigGroup.getInputShapeFileForServiceAreaAdjustment().equals("") || optDrtConfigGroup.getInputShapeFileForServiceAreaAdjustment().equals("null")) {
                throw new RuntimeException("opt drt input shape file for service area adjustment is 'null'. Aborting...");
            }
        }

        if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled) {
            if (optDrtConfigGroup.getInputShapeFileInitialServiceArea() == null || optDrtConfigGroup.getInputShapeFileInitialServiceArea().equals("null") || optDrtConfigGroup.getInputShapeFileInitialServiceArea().equals("")) {
                log.info("opt drt input shape file for initial service area is empty. Starting without any restriction regarding the drt service area...");
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (optDrtConfigGroup.getUpdateInterval() != 0
                && event.getIteration() != this.scenario.getConfig().controler().getLastIteration()
                && event.getIteration() <= optDrtConfigGroup.getUpdateEndFractionIteration() * this.scenario.getConfig().controler().getLastIteration()
                && event.getIteration() % optDrtConfigGroup.getUpdateInterval() == 0.) {

            log.info("Iteration " + event.getIteration() + ". Applying DRT strategies...");

            this.optDrtFareStrategy.updateFares();
            this.optDrtFleetStrategy.updateFleet();
            this.optDrtServiceAreaStrategy.updateServiceArea();

            log.info("Iteration " + event.getIteration() + ". Applying DRT strategies... Done.");

        }

        if (optDrtConfigGroup.getWriteInfoInterval() != 0
                && event.getIteration() % optDrtConfigGroup.getWriteInfoInterval() == 0.) {

            this.optDrtFareStrategy.writeInfo();
            this.optDrtFleetStrategy.writeInfo();
            this.optDrtServiceAreaStrategy.writeInfo();
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {

        if (optDrtConfigGroup.getUpdateInterval() > 1) {

            Set<String> subpopulations = new HashSet<>();
            for (StrategySettings setting : this.scenario.getConfig().strategy().getStrategySettings()) {
                subpopulations.add(setting.getSubpopulation());
                if (subpopulations.size() == 0) subpopulations.add(null);
            }

            if (event.getIteration() == this.scenario.getConfig().controler().getFirstIteration()) {

                this.nextDisableInnovativeStrategiesIteration = (int) (this.scenario.getConfig().strategy().getFractionOfIterationsToDisableInnovation() * optDrtConfigGroup.getUpdateInterval());
                log.info("next disable innovative strategies iteration: " + this.nextDisableInnovativeStrategiesIteration);

                if (this.nextDisableInnovativeStrategiesIteration != 0) {
                    this.nextEnableInnovativeStrategiesIteration = (int) (optDrtConfigGroup.getUpdateInterval() + 1);
                    log.info("next enable innovative strategies iteration: " + this.nextEnableInnovativeStrategiesIteration);
                }


            } else {

                if (event.getIteration() == this.nextDisableInnovativeStrategiesIteration) {

                    for (String subpopulation : subpopulations) {
                        for (GenericPlanStrategy<Plan, Person> genericPlanStrategy : strategyManager.getStrategies(subpopulation)) {
                            PlanStrategy planStrategy = (PlanStrategy) genericPlanStrategy;
                            if (isInnovativeStrategy(planStrategy)) {
                                log.info("Setting weight for " + planStrategy.toString() + " (subpopulation " + subpopulation + ") to 0.");
                                strategyManager.addChangeRequest(this.nextDisableInnovativeStrategiesIteration, planStrategy, subpopulation, 0.);
                            }
                        }
                    }

                    this.nextDisableInnovativeStrategiesIteration += optDrtConfigGroup.getUpdateInterval();
                    log.info("next disable innovative strategies iteration: " + this.nextDisableInnovativeStrategiesIteration);

                } else if (event.getIteration() == this.nextEnableInnovativeStrategiesIteration) {

                    if (event.getIteration() >= this.scenario.getConfig().strategy().getFractionOfIterationsToDisableInnovation() * (this.scenario.getConfig().controler().getLastIteration() - this.scenario.getConfig().controler().getFirstIteration())) {
                        log.info("Strategies are switched off by global settings. Do not set back the strategy parameters to original values...");

                    } else {
                        for (String subpopulation : subpopulations) {
                            for (GenericPlanStrategy<Plan, Person> genericPlanStrategy : strategyManager.getStrategies(subpopulation)) {
                            	if (isInnovativeStrategy(genericPlanStrategy)) {
                            		PlanStrategy planStrategy = (PlanStrategy) genericPlanStrategy;
                                    double originalWeight = Double.MIN_VALUE;
                                    for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
                                        PlanStrategy strategy = entry.getValue();
                                        StrategyConfigGroup.StrategySettings settings = entry.getKey();

                                        if (subpopulation.equals(settings.getSubpopulation()) && planStrategy.toString().equals(strategy.toString())) {
                                            originalWeight = settings.getWeight();
                                        }
                                    }

                                    if (originalWeight < 0.) {
                                        throw new RuntimeException("Can't set the innovative strategy's weight back to original value at the end of the inner iteration loop. Aborting...");
                                    }

                                    log.info("Setting weight for " + planStrategy.toString() + " (subpopuation " + subpopulation + ") back to original value: " + originalWeight);
                                    strategyManager.addChangeRequest(this.nextEnableInnovativeStrategiesIteration, planStrategy, subpopulation, originalWeight);
                            	}
                            }
                        }
                        this.nextEnableInnovativeStrategiesIteration += optDrtConfigGroup.getUpdateInterval();
                        log.info("next enable innovative strategies iteration: " + this.nextEnableInnovativeStrategiesIteration);
                    }
                }
            }
        }
    }

    private boolean isInnovativeStrategy(GenericPlanStrategy<Plan, Person> strategy) {
        boolean innovative = !(ReplanningUtils.isOnlySelector(strategy));
        return innovative;
    }
}
