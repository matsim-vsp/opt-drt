/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.optDRT;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class MultiModeOptDrtQSimModule extends AbstractQSimModule {
	private final MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup;

	public MultiModeOptDrtQSimModule(MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup) {
		this.multiModeOptDrtConfigGroup = multiModeOptDrtConfigGroup;
	}

	@Override
	protected void configureQSim() {
		for (OptDrtConfigGroup optDrtConfigGroup : multiModeOptDrtConfigGroup.getModalElements()) {
			install(new OptDrtQSimModule(optDrtConfigGroup));
		}
	}
}
