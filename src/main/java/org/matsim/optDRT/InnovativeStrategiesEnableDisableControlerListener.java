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
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningUtils;
import org.matsim.core.replanning.StrategyManager;

import com.google.inject.Inject;

/**
 * @author ikaddoura
 */
public class InnovativeStrategiesEnableDisableControlerListener implements IterationStartsListener {

    private static final Logger log = Logger.getLogger(InnovativeStrategiesEnableDisableControlerListener.class);

    @Inject
    private MultiModeOptDrtConfigGroup multiModeOptDrtCfg;

    @Inject
    private Scenario scenario;

    @Inject
    private Map<StrategyConfigGroup.StrategySettings, PlanStrategy> planStrategies;

    @Inject
    private StrategyManager strategyManager;

    private int nextDisableInnovativeStrategiesIteration = -1;
    private int nextEnableInnovativeStrategiesIteration = -1;

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {

        if (multiModeOptDrtCfg.getUpdateInterval() > 1) {

            Set<String> subpopulations = new HashSet<>();
            for (StrategySettings setting : this.scenario.getConfig().strategy().getStrategySettings()) {
                subpopulations.add(setting.getSubpopulation());
                if (subpopulations.size() == 0) subpopulations.add(null);
            }

            if (event.getIteration() == this.scenario.getConfig().controler().getFirstIteration()) {

                this.nextDisableInnovativeStrategiesIteration = (int) (this.scenario.getConfig().strategy().getFractionOfIterationsToDisableInnovation() * multiModeOptDrtCfg.getUpdateInterval());
                log.info("Next iteration in which innovative strategies are disabled: " + this.nextDisableInnovativeStrategiesIteration);

                if (this.nextDisableInnovativeStrategiesIteration != 0) {
                    this.nextEnableInnovativeStrategiesIteration = (int) (multiModeOptDrtCfg.getUpdateInterval() + 1);
                    log.info("Next iteration in which innovative strategies are enabled: " + this.nextEnableInnovativeStrategiesIteration);
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

                    this.nextDisableInnovativeStrategiesIteration += multiModeOptDrtCfg.getUpdateInterval();
                    log.info("Next iteration in which innovative strategies are disabled: " + this.nextDisableInnovativeStrategiesIteration);

                } else if (event.getIteration() == this.nextEnableInnovativeStrategiesIteration) {

                    if (event.getIteration() >= this.scenario.getConfig().strategy().getFractionOfIterationsToDisableInnovation() * (this.scenario.getConfig().controler().getLastIteration() - this.scenario.getConfig().controler().getFirstIteration())) {
                        log.info("Strategies are switched off by global settings. Do not set back the strategy parameters to original values...");

                    } else {
                    	
                    	double weightForInnovativeStrategies = -1.;
                    	boolean sameWeightForAllInnovativeStrategies = true;
                    	for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
                            PlanStrategy strategy = entry.getValue();
                            StrategyConfigGroup.StrategySettings settings = entry.getKey();
                            
                            if (!isInnovativeStrategy(strategy)) {
                            	// skip
                            } else {
                            	if (weightForInnovativeStrategies < 0) {
                                    weightForInnovativeStrategies = settings.getWeight();
                            	} else {
                            		if (weightForInnovativeStrategies != settings.getWeight()) {
                            			sameWeightForAllInnovativeStrategies = false;
                            		}
                            	}
                            }
                    	}
                    	
                    	if (sameWeightForAllInnovativeStrategies) {
                    		log.info("Same weight for all innovative strategies: " + weightForInnovativeStrategies);
                    		for (String subpopulation : subpopulations) {
                                for (GenericPlanStrategy<Plan, Person> genericPlanStrategy : strategyManager.getStrategies(subpopulation)) {
                                	if (isInnovativeStrategy(genericPlanStrategy)) {
                                		PlanStrategy planStrategy = (PlanStrategy) genericPlanStrategy;
                                        
                                        if (weightForInnovativeStrategies < 0.) {
                                            throw new RuntimeException("Can't set the innovative strategy's weight back to original value at the end of the inner iteration loop. Aborting...");
                                        }

                                        log.info("Setting weight for " + planStrategy.getClass().getName() + " / " + planStrategy.toString() + " (subpopuation " + subpopulation + ") back to original value: " + weightForInnovativeStrategies);
                                        strategyManager.addChangeRequest(this.nextEnableInnovativeStrategiesIteration, planStrategy, subpopulation, weightForInnovativeStrategies);
                                	}
                                }
                            }
                    	} else {
                    		log.warn("Different weights for innovative strategies... Please check the following warnings in the logfile.");
                    		for (String subpopulation : subpopulations) {
                                for (GenericPlanStrategy<Plan, Person> genericPlanStrategy : strategyManager.getStrategies(subpopulation)) {
                                	if (isInnovativeStrategy(genericPlanStrategy)) {
                                		PlanStrategy planStrategy = (PlanStrategy) genericPlanStrategy;
                                        
                                        log.warn("------------------------------------------");
                                        log.warn("Trying to identify the original weight for subpopulation " + subpopulation + " and strategy " + planStrategy.getClass().getName() + " / " + planStrategy.toString() + "...");

                                		double originalWeight = -1.;
                                        for (Map.Entry<StrategyConfigGroup.StrategySettings, PlanStrategy> entry : planStrategies.entrySet()) {
                                            PlanStrategy strategy = entry.getValue();
                                            StrategyConfigGroup.StrategySettings settings = entry.getKey();
                                            
                                            if (!isInnovativeStrategy(strategy)) {
                                            	// skip
                                            	log.warn("Skipping " + strategy.toString());
                                            } else {
                                            	log.warn("---");
                                                log.warn(" strategy.toString(): " + strategy.toString());
                                                log.warn(" strategy.getClass().getName(): " + strategy.getClass().getName());
                                                log.warn(" subpopulation: " + settings.getSubpopulation());
                                                
                                                boolean matchingSubpopulation = false;
                                                if (subpopulation == null && settings.getSubpopulation() == null ) {
                                                	// subpopulation is null
                                                	matchingSubpopulation = true;
                                                } else if (subpopulation.equals(settings.getSubpopulation())){
                                                	// same subpopulation
                                                	matchingSubpopulation = true;
                                                }
                                                
                                                if (matchingSubpopulation) {
                                                    if (planStrategy.toString().equals(strategy.toString())) {
                                                        originalWeight = settings.getWeight();
                                                        log.warn("Matching strategy found. Original weight: " + originalWeight);
                                                        break;
                                                    }
                                                }
                                            }
                                        }

                                        if (originalWeight < 0.) {
                                            throw new RuntimeException("Can't set the innovative strategy's weight back to original value at the end of the inner iteration loop. Aborting...");
                                        }

                                        log.info("Setting weight for " + planStrategy.getClass().getName() + " (subpopuation " + subpopulation + ") back to original value: " + originalWeight);
                                        strategyManager.addChangeRequest(this.nextEnableInnovativeStrategiesIteration, planStrategy, subpopulation, originalWeight);
                                	}
                                }
                            }
                    	}
                    	
                        this.nextEnableInnovativeStrategiesIteration += multiModeOptDrtCfg.getUpdateInterval();
                        log.info("Next iteration in which innovative strategies are enabled: " + this.nextEnableInnovativeStrategiesIteration);
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
