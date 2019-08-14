/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.run.berlin;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.run.BerlinMainModeIdentifier;

import com.google.inject.Inject;

/**
 * @author ikaddoura
 */
public final class BerlinDrtMainModeIdentifier implements MainModeIdentifier {
	
	private final BerlinMainModeIdentifier delegate = new BerlinMainModeIdentifier();
	private final String mode;
	private final DrtStageActivityType drtStageActivityType;
	
	@Inject
	public BerlinDrtMainModeIdentifier(DrtConfigGroup drtCfg) {
		mode = drtCfg.getMode();
		drtStageActivityType = new DrtStageActivityType(drtCfg.getMode());
	}
	
	@Override
	public String identifyMainMode( final List<? extends PlanElement> tripElements) {
		
		for (PlanElement pe : tripElements) {
			if (pe instanceof Activity) {
				if (((Activity) pe).getType().equals(drtStageActivityType.drtStageActivity))
					return mode;
			} else if (pe instanceof Leg) {
				if (((Leg) pe).getMode().equals(drtStageActivityType.drtWalk)) {
					return mode;
				}
			}
		}
		
		return delegate.identifyMainMode(tripElements);
		
	}
}
