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

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class MultiModeOptDrtModule extends AbstractModule {

	@Inject
	private MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup;

	@Override
	public void install() {	
		
		for (OptDrtConfigGroup optDrtConfigGroup : multiModeOptDrtConfigGroup.getModalElements()) {
			install(new OptDrtModule(optDrtConfigGroup));
		}
	}

}

