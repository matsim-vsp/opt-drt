/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.List;

/**
* @author vsp-gleich
*/

class OptDrtFleetModifierByPureRandom implements OptDrtFleetModifier {
	private static final Logger log = Logger.getLogger(OptDrtFleetModifierByPureRandom.class);

	private final FleetSpecification fleetSpecification;

	int vehicleCounter = 0;

	public OptDrtFleetModifierByPureRandom(FleetSpecification fleetSpecification) {
		this.fleetSpecification = fleetSpecification;
	}

	public void decreaseFleet( int vehiclesToRemove ) {
		
		log.info("Removing " + vehiclesToRemove + " vehicles...");

		List<Id<DvrpVehicle>> dvrpVehiclesBefore = new ArrayList<>();
		for (DvrpVehicleSpecification specification : fleetSpecification.getVehicleSpecifications().values()) {
			dvrpVehiclesBefore.add(specification.getId());
		}

		List<Id<DvrpVehicle>> dvrpVehiclesToRemove = new ArrayList<>();		
		for (int v = 0; v <= vehiclesToRemove; v++) {
			if (dvrpVehiclesBefore.size() > 0) {
				final int randomVehicleNr = (int) (dvrpVehiclesBefore.size() * MatsimRandom.getLocalInstance().nextDouble());
				dvrpVehiclesToRemove.add(dvrpVehiclesBefore.remove(randomVehicleNr));
			}
		}
		
		for (Id<DvrpVehicle> id : dvrpVehiclesToRemove) {
			if (fleetSpecification.getVehicleSpecifications().size() > 1) {
				// always keep one drt 'mother' vehicle
				fleetSpecification.removeVehicleSpecification(id);
				log.info("Removing dvrp vehicle " + id); 
			}
		}
		
		int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();

		log.info("Dvrp vehicle fleet was decreased to " + vehiclesAfter);
	}

	public void increaseFleet( int vehiclesToAdd ) {
		if ( fleetSpecification.getVehicleSpecifications().keySet().size() < 1 ) {
			throw new RuntimeException("No dvrp vehicle found to be cloned. Maybe create some default dvrp vehicle which is specified somewhere. Aborting...");
		}
		log.info("Adding " + vehiclesToAdd + " vehicles using FleetUpdateVehicleSelection " + OptDrtConfigGroup.FleetUpdateVehicleSelection.Random);
		for (int i = 0; i < vehiclesToAdd; i++) {
			cloneAndAddDvrpVehicle(getRandomVehicleSpecification());
		}
		
		int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();
		
		log.info("Dvrp vehicle fleet was increased to " + vehiclesAfter);

	}

	private void cloneAndAddDvrpVehicle(DvrpVehicleSpecification dvrpVehicleSpecficationToBeCloned) {
		Id<DvrpVehicle> id = Id.create("optDrt_" + vehicleCounter + "_cloneOf_" + dvrpVehicleSpecficationToBeCloned.getId(), DvrpVehicle.class);
		DvrpVehicleSpecification newSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
				.id(id)
				.serviceBeginTime(dvrpVehicleSpecficationToBeCloned.getServiceBeginTime())
				.serviceEndTime(dvrpVehicleSpecficationToBeCloned.getServiceEndTime())
				.startLinkId(dvrpVehicleSpecficationToBeCloned.getStartLinkId())
				.capacity(dvrpVehicleSpecficationToBeCloned.getCapacity())
				.build();

		fleetSpecification.addVehicleSpecification(newSpecification);
		log.info("Adding dvrp vehicle " + id);

		vehicleCounter++;
	}

	private DvrpVehicleSpecification getRandomVehicleSpecification() {
		DvrpVehicleSpecification dvrpVehicleSpecficationToBeCloned = null;
		final int randomVehicleNr = (int) (fleetSpecification.getVehicleSpecifications().size() * MatsimRandom.getLocalInstance().nextDouble());

		int counter = 0;
		for (DvrpVehicleSpecification specification : fleetSpecification.getVehicleSpecifications().values()) {
			if (counter == randomVehicleNr) {
				dvrpVehicleSpecficationToBeCloned = specification;
			}
			counter++;
		}
		return dvrpVehicleSpecficationToBeCloned;
	}

}

