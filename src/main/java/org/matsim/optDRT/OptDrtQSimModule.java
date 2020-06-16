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

import static org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach.DemandThreshold;
import static org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach.Disabled;

import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;

/**
 * @author ikaddoura
 */

public class OptDrtQSimModule extends AbstractDvrpModeQSimModule {

	private final OptDrtConfigGroup optDrtConfigGroup;

	public OptDrtQSimModule(OptDrtConfigGroup optDrtConfigGroup) {
		super(optDrtConfigGroup.getMode());
		this.optDrtConfigGroup = optDrtConfigGroup;
	}

	@Override
	protected void configureQSim() {

		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == Disabled) {
			// disabled
		} else if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == DemandThreshold) {
			bindModal(PassengerRequestValidator.class).to(modalKey(OptDrtServiceAreaStrategyDemand.class));
		} else {
			throw new RuntimeException("Unknown service area adjustment approach. Aborting...");
		}
	}
}

