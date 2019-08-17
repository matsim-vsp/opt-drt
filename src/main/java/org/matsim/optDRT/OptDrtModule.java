/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.optDRT;

import org.matsim.core.controler.AbstractModule;
import org.matsim.optDRT.OptDrtConfigGroup.FareAdjustmentApproach;
import org.matsim.optDRT.OptDrtConfigGroup.FleetSizeAdjustmentApproach;
import org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class OptDrtModule extends AbstractModule {

	@Inject
	private OptDrtConfigGroup optDrtConfigGroup;

	@Override
	public void install() {		
		
		// dynamic fare strategy
		if (optDrtConfigGroup.getFareAdjustmentApproach() == FareAdjustmentApproach.Disabled) {
			// disabled
		} else if (optDrtConfigGroup.getFareAdjustmentApproach() == FareAdjustmentApproach.AverageWaitingTimeThreshold) {
			this.bind(OptDrtFareStrategyWaitingTime.class).asEagerSingleton();
			this.bind(OptDrtFareStrategy.class).to(OptDrtFareStrategyWaitingTime.class);
			this.addEventHandlerBinding().to(OptDrtFareStrategyWaitingTime.class);	
		} else {
			throw new RuntimeException("Unknown fare adjustment approach. Aborting...");
		}
		
		// fleet size strategy
		if (optDrtConfigGroup.getFleetSizeAdjustmentApproach() == FleetSizeAdjustmentApproach.Disabled) {
			// disabled
		} else if (optDrtConfigGroup.getFleetSizeAdjustmentApproach() == FleetSizeAdjustmentApproach.ProfitThreshold) {
			this.bind(OptDrtFleetStrategyProfit.class).asEagerSingleton();
			this.bind(OptDrtFleetStrategy.class).to(OptDrtFleetStrategyProfit.class);
			this.addEventHandlerBinding().to(OptDrtFleetStrategyProfit.class);
			
			// Right now, this module only works for a single mode specified in OptDrtConfigGroup. Makes everything much nicer and easier. 
			// At some point we might think about a modal binding and extend AbstractDvrpModeModule instead of AbstractModule...
			// ... and do approximately the following.   ihab June '19	
			//
	        // bindModal(OptDrtFleetStrategy.class).toProvider(modalProvider(getter ->
	        // new OptDrtFleetStrategyAddRemove(getter.getModal(FleetSpecification.class), getMode(), this.optDrtCfg))).asEagerSingleton();
	        // bindModal(OptDrtControlerListener.class).toProvider(modalProvider(t->new OptDrtControlerListener(t.getModal(OptDrtFleetStrategy.class), t.get(Scenario.class)))).asEagerSingleton();
	 		// addControlerListenerBinding().to(modalKey(OptDrtControlerListener.class));
			
		} else if (optDrtConfigGroup.getFleetSizeAdjustmentApproach() == FleetSizeAdjustmentApproach.AverageWaitingTimeThreshold) {
			this.bind(OptDrtFleetStrategyWaitingTime.class).asEagerSingleton();
			this.bind(OptDrtFleetStrategy.class).to(OptDrtFleetStrategyWaitingTime.class);
			this.addEventHandlerBinding().to(OptDrtFleetStrategyWaitingTime.class);
		} else {
			throw new RuntimeException("Unknown fleet size adjustment approach. Aborting...");
		}
		
		// service area strategy
		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == ServiceAreaAdjustmentApproach.Disabled) {
			// disabled
		} else if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == ServiceAreaAdjustmentApproach.DemandThreshold) {				
			this.bind(OptDrtServiceAreaStrategyDemand.class).asEagerSingleton();
			this.bind(OptDrtServiceAreaStrategy.class).to(OptDrtServiceAreaStrategyDemand.class);
			this.addEventHandlerBinding().to(OptDrtServiceAreaStrategyDemand.class);
			this.addControlerListenerBinding().to(OptDrtServiceAreaStrategyDemand.class);		
		} else {
			throw new RuntimeException("Unknown service area adjustment approach. Aborting...");
		}
				
		addControlerListenerBinding().to(OptDrtControlerListener.class);	
	}

}

