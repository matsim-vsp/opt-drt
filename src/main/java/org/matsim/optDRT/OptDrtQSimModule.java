/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach;

/**
* @author ikaddoura
*/

public class OptDrtQSimModule extends AbstractDvrpModeQSimModule {

	private final OptDrtConfigGroup optDrtConfigGroup;

	public OptDrtQSimModule(String mode, OptDrtConfigGroup optDrtConfigGroup) {
		super(mode);
		this.optDrtConfigGroup = optDrtConfigGroup;
	}

	@Override
	protected void configureQSim() {
		
		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == ServiceAreaAdjustmentApproach.Disabled) {
			// disabled
			
		} else if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == ServiceAreaAdjustmentApproach.DemandThreshold) {				
			this.bindModal(PassengerRequestValidator.class).to(OptDrtServiceAreaStrategyDemand.class);
			
		} else {
			throw new RuntimeException("Unknown service area adjustment approach. Aborting...");
		}
	}

}

