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

package org.matsim.run.berlin;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.Trip;

/**
* @author ikaddoura
*/

public final class PersonAttributesModification {

	private final BerlinShpUtils shpUtils;
	private final StageActivityTypes stageActivities;

	public PersonAttributesModification(BerlinShpUtils shpUtils, StageActivityTypes stageActivities) {
		this.shpUtils = shpUtils;
		this.stageActivities = stageActivities;
	}

	public void run(Scenario scenario) {
		
		for (Person person : scenario.getPopulation().getPersons().values()) {
			
			if (scenario.getPopulation().getPersonAttributes().getAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName()).equals("freight")) {
				// skip freight agents
			} else {
				boolean personHasAtLeastOneTripWithinServiceArea = false;
				
				for (Trip trip : TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements(), stageActivities)) {
					if (shpUtils.isCoordInDrtServiceArea(trip.getOriginActivity().getCoord()) && shpUtils.isCoordInDrtServiceArea(trip.getDestinationActivity().getCoord())) {
						// trip in berlin city area
						personHasAtLeastOneTripWithinServiceArea = true;
						break;
					}
				}
				
				if (personHasAtLeastOneTripWithinServiceArea) {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_potential-sav-user");
				} else {
					scenario.getPopulation().getPersonAttributes().putAttribute(person.getId().toString(), scenario.getConfig().plans().getSubpopulationAttributeName(), "person_no-potential-sav-user");
				}
			}
		}
	}

}

