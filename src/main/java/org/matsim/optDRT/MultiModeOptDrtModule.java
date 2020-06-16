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

import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.core.controler.AbstractModule;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

/**
 * @author ikaddoura
 */

public class MultiModeOptDrtModule extends AbstractModule {

	@Inject
	private MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup;

	@Inject
	private DrtFaresConfigGroup drtFaresConfigGroup;

	@Override
	public void install() {
		ImmutableMap<String, DrtFareConfigGroup> drtFaresConfigs = drtFaresConfigGroup.getDrtFareConfigGroups()
				.stream()
				.collect(ImmutableMap.toImmutableMap(DrtFareConfigGroup::getMode, cfg -> cfg));
		for (OptDrtConfigGroup optDrtConfigGroup : multiModeOptDrtConfigGroup.getModalElements()) {
			install(new OptDrtModule(optDrtConfigGroup, drtFaresConfigs.get(optDrtConfigGroup.getMode())));
		}
	}
}

