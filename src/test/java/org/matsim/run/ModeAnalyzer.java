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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;

/**
* @author ikaddoura zmeng
*/

public class ModeAnalyzer implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler {

	private final Set<Id<Person>> drtPassengers = new HashSet<>();
	private int enteredDrtVehicles = 0;
	private Map<Integer, Integer> it2enteredDrtPassengers = new HashMap<>();

	private final Set<Id<Person>> taxiPassengers = new HashSet<>();
	private int enteredTaxiVehicles = 0;
	private Map<Integer, Integer> it2enteredTaxiPassengers = new HashMap<>();

	private int currentIteration = 0;

	@Override
	public void reset(int iteration) {
		this.drtPassengers.clear();
		this.enteredDrtVehicles = 0;
		this.currentIteration = iteration;
		this.it2enteredDrtPassengers.put(currentIteration, 0);

		this.taxiPassengers.clear();
		this.enteredTaxiVehicles = 0;
		this.it2enteredTaxiPassengers.put(currentIteration, 0);
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

		if (event.getLegMode().equals(TransportMode.taxi)) {
			taxiPassengers.add(event.getPersonId());
		} else {
			if (taxiPassengers.contains(event.getPersonId())) {
				taxiPassengers.remove(event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (drtPassengers.contains(event.getPersonId())) {
			enteredDrtVehicles++;
			int oldDrtNumber = this.it2enteredDrtPassengers.get(currentIteration);
			this.it2enteredDrtPassengers.put(currentIteration, oldDrtNumber + 1);
		}

		if (taxiPassengers.contains(event.getPersonId())) {
			enteredTaxiVehicles++;
			int oldTaxiNummber = this.it2enteredTaxiPassengers.get(currentIteration);
			this.it2enteredTaxiPassengers.put(currentIteration, oldTaxiNummber + 1);
		}
	}

	public int getEnteredDrtVehicles() {
		return enteredDrtVehicles;
	}

	public int getEnteredTaxiVehicles() {
		return enteredTaxiVehicles;
	}

	public Map<Integer, Integer> getIt2enteredDrtPassengers() {
		return it2enteredDrtPassengers;
	}

	public Map<Integer, Integer> getIt2enteredTaxiPassengers() {
		return it2enteredTaxiPassengers;
	}

}

