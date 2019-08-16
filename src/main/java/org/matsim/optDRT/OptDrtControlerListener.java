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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach;

import com.google.inject.Inject;


/**
 * 
 * @author ikaddoura
 *
 */

public class OptDrtControlerListener implements StartupListener, IterationEndsListener {
		
	private static final Logger log = Logger.getLogger(OptDrtControlerListener.class);
	
	@Inject
	private OptDrtConfigGroup optDrtConfigGroup;
	
	@Inject(optional=true)
	private OptDrtFareStrategy optDrtFareStrategy;
	
	@Inject(optional=true)
	private OptDrtFleetStrategy optDrtFleetStrategy;
	
	@Inject(optional=true)
	private OptDrtServiceAreaStrategy optDrtServiceAreaStrategy;
		
	@Inject
	Scenario scenario;
		
	@Override
	public void notifyStartup(StartupEvent event) {	
		log.info("optDrt settings: " + optDrtConfigGroup.toString());
		
		if (!optDrtConfigGroup.getOptDrtMode().equals("drt")) {
			throw new RuntimeException("Currently, a mode different from 'drt' is not allowed."
					+ " Only works for a single mode specified in OptDrtConfigGroup. At some point we might think about a modal binding."
					+ " Aborting... ");
		}
		
		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled && optDrtConfigGroup.getInputShapeFileForServiceAreaAdjustment() == "") {
			throw new RuntimeException("opt drt input shape file for service area adjustment is 'null'. Aborting...");
		}
		
		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled && optDrtConfigGroup.getInputShapeFileInitialServiceArea() == null) {
			log.info("opt drt input shape file for initial service area is empty. Starting without any restriction regarding the drt service area...");
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
				
		if (optDrtConfigGroup.getUpdateInterval() != 0
				&& event.getIteration() != this.scenario.getConfig().controler().getLastIteration()
				&& event.getIteration() <= optDrtConfigGroup.getUpdateEndFractionIteration() * this.scenario.getConfig().controler().getLastIteration()
				&& event.getIteration() % optDrtConfigGroup.getUpdateInterval() == 0.) {
			
			log.info("Iteration " + event.getIteration() + ". Applying DRT strategies...");
			
			if (this.optDrtFareStrategy != null) this.optDrtFareStrategy.updateFares();
			if (this.optDrtFleetStrategy != null) this.optDrtFleetStrategy.updateFleet();
			if (this.optDrtServiceAreaStrategy != null) this.optDrtServiceAreaStrategy.updateServiceArea();

			log.info("Iteration " + event.getIteration() + ". Applying DRT strategies... Done.");
			
		}
		
		if (optDrtConfigGroup.getWriteInfoInterval() != 0
				&& event.getIteration() % optDrtConfigGroup.getWriteInfoInterval() == 0.) {
			
			if (this.optDrtFareStrategy != null) this.optDrtFareStrategy.writeInfo();;
			if (this.optDrtFleetStrategy != null) this.optDrtFleetStrategy.writeInfo();
			if (this.optDrtServiceAreaStrategy != null) this.optDrtServiceAreaStrategy.writeInfo();
		}
	}
}
