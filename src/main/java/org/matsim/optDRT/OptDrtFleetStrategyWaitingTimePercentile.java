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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.core.config.Config;
import org.matsim.optDRT.OptDrtConfigGroup.FleetUpdateApproach;

/**
* @author ikaddoura
*/

class OptDrtFleetStrategyWaitingTimePercentile implements OptDrtFleetStrategy, PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtFleetStrategyWaitingTimePercentile.class);

	private final FleetSpecification fleetSpecification;

	private final OptDrtConfigGroup optDrtConfigGroup;

	private final Config config;

	private final OptDrtFleetModifier fleetModifier;

	private final Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
	private final List<Double> waitingTimes = new ArrayList<>();
	
	private List<String> iterationStatistics = new ArrayList<>();

	public OptDrtFleetStrategyWaitingTimePercentile(FleetSpecification fleetSpecification,
			OptDrtConfigGroup optDrtConfigGroup, Config config, OptDrtFleetModifier fleetModifier) {
		this.fleetSpecification = fleetSpecification;
		this.optDrtConfigGroup = optDrtConfigGroup;
		this.config = config;
		this.fleetModifier = fleetModifier;
		iterationStatistics.add("RunId;Iteration;fleetSize;waitingTime-" + optDrtConfigGroup.getTripShareThresholdForFleetSizeAdjustment() + "-percentile;targetWaitingTime-" + optDrtConfigGroup.getTripShareThresholdForFleetSizeAdjustment() + "-percentile;numberOfTripsWithWaitingTimeAboveThreshold;numberOfTripsWithWaitingTimeBelowOrEqualsThreshold");
	}

	@Override
	public void reset(int iteration) {}

	@Override
	public void resetDataForThisIteration( int currentIteration ) {
		drtUserDepartureTime.clear();
		waitingTimes.clear();
	}

	@Override
	public void updateFleet( int currentIteration ) {
		
		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();		
		log.info("Current fleet size: " + vehiclesBefore);
		
		double currentWaitingTimePercentile = computeWaitingTimePercentile();
		double targetWaitingTimePercentile = optDrtConfigGroup.getWaitingTimeThresholdForFleetSizeAdjustment();

		log.info("currentWaitingTimePercentile: " + currentWaitingTimePercentile);
		log.info("targetWaitingTimePercentile: " + targetWaitingTimePercentile);
		
		if (Double.isNaN(currentWaitingTimePercentile)) {
			log.info("current waiting time percentile is NaN. Fleet size will not be adjusted.");
		} else {
			if (currentWaitingTimePercentile > targetWaitingTimePercentile) {
				
				int vehiclesToAdd = 0;
				if (optDrtConfigGroup.getFleetUpdateApproach() == FleetUpdateApproach.BangBang) {				
					int vehiclesToAddFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
					int vehiclesToAddFromRelativeNumber = (int) (optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore) ;
					vehiclesToAdd = Math.max(vehiclesToAddFromAbsoluteNumber, vehiclesToAddFromRelativeNumber);
				} else if (optDrtConfigGroup.getFleetUpdateApproach() == FleetUpdateApproach.Proportional) {
					int vehiclesToAddFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
					double relativePercentileDifference = (currentWaitingTimePercentile - targetWaitingTimePercentile) / targetWaitingTimePercentile ;				
					int vehiclesToAddFromRelativeNumber = (int) (relativePercentileDifference * optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore);
					vehiclesToAdd = Math.max(vehiclesToAddFromAbsoluteNumber, vehiclesToAddFromRelativeNumber);
				} else {
					throw new RuntimeException("Unknown fleet update approach. Aborting...");
				}

				fleetModifier.increaseFleet(vehiclesToAdd);
				
			} else {
					
				int vehiclesToRemove = 0;
				
				if (optDrtConfigGroup.getFleetUpdateApproach() == FleetUpdateApproach.BangBang) {				
					int vehiclesToRemoveFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
					int vehiclesToRemoveFromRelativeNumber = (int) (optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore) ;
					vehiclesToRemove = Math.max(vehiclesToRemoveFromAbsoluteNumber, vehiclesToRemoveFromRelativeNumber);
					
				} else if (optDrtConfigGroup.getFleetUpdateApproach() == FleetUpdateApproach.Proportional) {
					int vehiclesToRemoveFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
					double relativePercentileDifference = -1. * (currentWaitingTimePercentile - targetWaitingTimePercentile) / targetWaitingTimePercentile ;				
					int vehiclesToRemoveFromRelativeNumber = (int) (relativePercentileDifference * optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore);
					vehiclesToRemove = Math.max(vehiclesToRemoveFromAbsoluteNumber, vehiclesToRemoveFromRelativeNumber);
					
				} else {
					throw new RuntimeException("Unknown fleet update approach. Aborting...");
				}
				
				fleetModifier.decreaseFleet(vehiclesToRemove);
			}
			OptDrtUtils.writeModifiedFleet(fleetSpecification, config, currentIteration, this.optDrtConfigGroup.getMode());
		}
	}

	private double computeWaitingTimePercentile() {
		double percentage = optDrtConfigGroup.getTripShareThresholdForFleetSizeAdjustment(); // e.g. 0.9 for a 90% percentile
		DescriptiveStatistics waitStats = new DescriptiveStatistics();

		for (Double waitingTime : this.waitingTimes) {
			waitStats.addValue(waitingTime);
		}
		double percentile = waitStats.getPercentile(percentage * 100);
		return percentile;
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {	
		if (this.drtUserDepartureTime.get(event.getPersonId()) != null) {	
			double waitingTime = event.getTime() - this.drtUserDepartureTime.get(event.getPersonId());
			waitingTimes.add(waitingTime);	
		}
	}
	
	@Override
    public void handleEvent(PersonArrivalEvent event) {  	
        if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {
			this.drtUserDepartureTime.remove(event.getPersonId());
        }
    }
	
	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {
			this.drtUserDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void writeInfo( int currentIteration ) {
		String runOutputDirectory = this.config.controler().getOutputDirectory();
		if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");

		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();

		double currentWaitingTimePercentile = computeWaitingTimePercentile();
		double targetWaitingTimePercentile = optDrtConfigGroup.getWaitingTimeThresholdForFleetSizeAdjustment();

		int cntAboveThreshold = 0;
		int cntBelowOrEqualsThreshold = 0;

		for (Double waitingTime : this.waitingTimes) {
			if (waitingTime > targetWaitingTimePercentile) {
				cntAboveThreshold++;
			} else {
				cntBelowOrEqualsThreshold++;
			}
		}

		String line = this.config.controler().getRunId() + ";" + currentIteration + ";" + vehiclesBefore + ";" + currentWaitingTimePercentile + ";" + targetWaitingTimePercentile + ";" + cntAboveThreshold + ";" + cntBelowOrEqualsThreshold;
		iterationStatistics.add(line);

		{
			String fileName = runOutputDirectory + "ITERS/it." + currentIteration + "/" + this.config.controler().getRunId() + "." + currentIteration + ".info_" + this.getClass().getName() + "_" + this.optDrtConfigGroup.getMode() + ".csv";
			File file = new File(fileName);			

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				
				double shareOfTripsAboveWaitingTimeThreshold = (double) cntAboveThreshold / (double) (cntAboveThreshold + cntBelowOrEqualsThreshold);
				bw.write("share of trips above waiting time threshold;" + shareOfTripsAboveWaitingTimeThreshold);
				bw.newLine();
				bw.write("trips above waiting time threshold;" + cntAboveThreshold);
				bw.newLine();
				bw.write("trips below or equals waiting time threshold;" + cntBelowOrEqualsThreshold);
				bw.newLine();
				bw.write("------------------------------------------------");
				bw.newLine();
				
				for (Double waitingTime : this.waitingTimes) {	
					bw.write(String.valueOf(waitingTime));
					bw.newLine();
				}
				log.info("Output written to " + fileName);
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		{
			String fileName = runOutputDirectory + this.config.controler().getRunId() + ".info_" + this.getClass().getName() + "_" + this.optDrtConfigGroup.getMode() + ".csv";
			File file = new File(fileName);			

			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(file));
				
				for (String line1 : this.iterationStatistics) {
					bw.write(line1);
					bw.newLine();
				}
				log.info("Output written to " + fileName);
				bw.close();
				
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}

