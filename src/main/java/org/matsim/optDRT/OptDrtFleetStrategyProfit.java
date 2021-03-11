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
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestScheduledEventHandler;

/**
 * @author ikaddoura
 */

class OptDrtFleetStrategyProfit
		implements OptDrtFleetStrategy, PersonMoneyEventHandler, PersonDepartureEventHandler, LinkLeaveEventHandler,
		PassengerRequestScheduledEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtFleetStrategyProfit.class);

	private final FleetSpecification fleetSpecification;

	private final OptDrtConfigGroup optDrtConfigGroup;

	private final Scenario scenario;

	private final OptDrtFleetModifier fleetModifier;

	private final Set<Id<Person>> departedDrtUsers = new HashSet<>();
	private double drtFareSum = 0.;
	private double drtVehDistance_m = 0.;
	private final Set<Id<DvrpVehicle>> drtVehicleIds = new HashSet<>();

	public OptDrtFleetStrategyProfit(FleetSpecification fleetSpecification, OptDrtConfigGroup optDrtConfigGroup,
			Scenario scenario, OptDrtFleetModifier fleetModifier) {
		this.fleetSpecification = fleetSpecification;
		this.optDrtConfigGroup = optDrtConfigGroup;
		this.scenario = scenario;
		this.fleetModifier = fleetModifier;
	}

	@Override
	public void reset(int iteration) {}

	@Override
	public void resetDataForThisIteration( int currentIteration ) {
		this.departedDrtUsers.clear();
		this.drtFareSum = 0.;
		this.drtVehDistance_m = 0.;
		this.drtVehicleIds.clear();
	}

	@Override
	public void updateFleet( int currentIteration ) {
		
		if (computeProfit() >= optDrtConfigGroup.getProfitThresholdForFleetSizeAdjustment()) {
			fleetModifier.increaseFleet(optDrtConfigGroup.getFleetSizeAdjustment());
		} else {
			fleetModifier.decreaseFleet(optDrtConfigGroup.getFleetSizeAdjustment());
		}
		OptDrtUtils.writeModifiedFleet(fleetSpecification, scenario.getConfig(), currentIteration, this.optDrtConfigGroup.getMode());
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
	public void writeInfo( int currentIteration ) {
		// TODO Auto-generated method stub
	}

}

