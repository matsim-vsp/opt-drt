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

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;
import org.matsim.contrib.dvrp.run.DvrpMode;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

class OptDrtFleetStrategyProfit implements OptDrtFleetStrategy, PersonMoneyEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler, PassengerRequestScheduledEventHandler{
	private static final Logger log = Logger.getLogger(OptDrtFleetStrategyProfit.class);
	
	@Inject
	@DvrpMode("drt")
	private FleetSpecification fleetSpecification;
	// Right now, OptDrtModule only works for a single mode specified in OptDrtConfigGroup. Makes everything much nicer and easier. 
	// At some point we might think about a modal binding and extend AbstractDvrpModeModule instead of AbstractModule... Ihab June '19	
	
	@Inject
	private OptDrtConfigGroup optDrtConfigGroup;
	
	@Inject
	private Scenario scenario;
	
	private int vehicleCounter = 0;
	
	private Set<Id<Person>> departedDrtUsers = new HashSet<>();
	private double drtFareSum = 0.;
	private double drtVehDistance_m = 0.;
	private Set<Id<DvrpVehicle>> drtVehicleIds = new HashSet<>();
	
	@Override
	public void reset(int iteration) {
		this.departedDrtUsers.clear();
		this.drtFareSum = 0.;
		this.drtVehDistance_m = 0.;
		this.drtVehicleIds.clear();
		
    	// do not reset vehicle counter
	}

	@Override
	public void updateFleet() {
		
		if (computeProfit() >= optDrtConfigGroup.getProfitThresholdForFleetSizeAdjustment()) {
			increaseFleet();
		} else {
			decreaseFleet();
		}
	}

	private void decreaseFleet() {
		
		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();

		Set<Id<DvrpVehicle>> dvrpVehiclesToRemove = new HashSet<>();
		int counter = 0;
		for (DvrpVehicleSpecification specification : fleetSpecification.getVehicleSpecifications().values()) {
			if (counter < optDrtConfigGroup.getFleetSizeAdjustment()) {
				dvrpVehiclesToRemove.add(specification.getId());
				counter++;
			}
		}
		
		for (Id<DvrpVehicle> id : dvrpVehiclesToRemove) {
			if (fleetSpecification.getVehicleSpecifications().size() > 1) {
				fleetSpecification.removeVehicleSpecification(id);
				log.info("Removing dvrp vehicle " + id); 
			}
		}
		
		int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();

		log.info("Dvrp vehicle fleet was decreased from " + vehiclesBefore + " to " + vehiclesAfter);
	}

	private void increaseFleet() {
		
		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();
		
		// select a random fleet specification to be cloned.
		DvrpVehicleSpecification dvrpVehicleSpecficationToBeCloned = null;
		final int randomVehicleNr = (int) (fleetSpecification.getVehicleSpecifications().size() * MatsimRandom.getLocalInstance().nextDouble());

		int counter = 0;
		for (DvrpVehicleSpecification specification : fleetSpecification.getVehicleSpecifications().values()) {
			if (counter == randomVehicleNr) {
				dvrpVehicleSpecficationToBeCloned = specification;
			}
			counter++;
		}
		
		if (dvrpVehicleSpecficationToBeCloned == null) {
			throw new RuntimeException("No dvrp vehicle found to be cloned. Maybe create some default dvrp vehicle which is specified somewhere. Aborting...");
		}

		for (int i = 0; i < optDrtConfigGroup.getFleetSizeAdjustment(); i++) {
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
		
		int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();
		
		log.info("Dvrp vehicle fleet was increased from " + vehiclesBefore + " to " + vehiclesAfter);

	}

	private double computeProfit() {			
		double cost = optDrtConfigGroup.getCostPerVehPerDayForFleetAdjustment() * fleetSpecification.getVehicleSpecifications().size()
				+  optDrtConfigGroup.getCostPerVehPerMeterForFleetAdjustment() * this.drtVehDistance_m;
		
		double revenues = this.drtFareSum;
		
		double profit = revenues - cost;
		
		log.info("Revenues: " + revenues + " /// Cost: " + cost + " /// Profit: " + profit);
		return profit;
	}

	@Override
	public void handleEvent(PersonMoneyEvent event) {
		if (this.departedDrtUsers.contains(event.getPersonId())) this.drtFareSum += (-1. * event.getAmount());
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {
			this.departedDrtUsers.add(event.getPersonId());
		} else {
			if (this.departedDrtUsers.contains(event.getPersonId())) this.departedDrtUsers.remove(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		if (this.drtVehicleIds.contains(event.getVehicleId())) this.drtVehDistance_m += scenario.getNetwork().getLinks().get(event.getLinkId()).getLength();
	}

	@Override
	public void handleEvent(PassengerRequestScheduledEvent event) {
		if (event.getMode().equals(optDrtConfigGroup.getMode())) {
			this.drtVehicleIds.add(event.getVehicleId());
		}
	}

	@Override
	public void writeInfo() {
		// TODO Auto-generated method stub
	}

}

