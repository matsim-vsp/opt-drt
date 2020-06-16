/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
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
 * *********************************************************************** */

package org.matsim.optDRT;

import java.util.Collection;

import javax.validation.Valid;

import org.matsim.contrib.dvrp.run.MultiModal;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
* @author ikaddoura
*/

public final class MultiModeOptDrtConfigGroup extends ReflectiveConfigGroup implements MultiModal<OptDrtConfigGroup>{
	public static final String GROUP_NAME = "multiModeOptDrt";
	
	private static final String UPDATE_INTERVAL = "optDrtUpdateInterval";
	private int updateInterval = 1;

	public MultiModeOptDrtConfigGroup() {
		super(GROUP_NAME);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Collection<@Valid OptDrtConfigGroup> getModalElements() {
		return (Collection<OptDrtConfigGroup>)getParameterSets(OptDrtConfigGroup.GROUP_NAME);
	}
	
	@Override
	public ConfigGroup createParameterSet(String type) {
		if (type.equals(OptDrtConfigGroup.GROUP_NAME)) {
			return new OptDrtConfigGroup();
		}
		throw new IllegalArgumentException(type);
	}

	@StringGetter( UPDATE_INTERVAL )
	public int getUpdateInterval() {
		return updateInterval;
	}
	
	@StringSetter( UPDATE_INTERVAL )
	public void setUpdateInterval(int uPDATE_INTERVAL) {
		updateInterval = uPDATE_INTERVAL;
	}

}

