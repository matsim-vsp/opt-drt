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

package org.matsim.run;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
* @author ikaddoura
*/

public class ModeAnalyzer implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {

	private final Set<Id<Person>> drtPassengers = new HashSet<>();
	private int enteredDrtVehicles = 0;
	
	@Override
	public void reset(int iteration) {
		this.drtPassengers.clear();
		this.enteredDrtVehicles = 0;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(TransportMode.drt)) {
			drtPassengers.add(event.getPersonId());
		} else {
			if (drtPassengers.contains(event.getPersonId())) {
				drtPassengers.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (drtPassengers.contains(event.getPersonId())) enteredDrtVehicles++;
	}

	public int getEnteredDrtVehicles() {
		return enteredDrtVehicles;
	}

}

