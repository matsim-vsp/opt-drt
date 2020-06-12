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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;

/**
* @author ikaddoura
*/

class OptDrtFleetStrategyWaitingTimePercentile implements OptDrtFleetStrategy, PersonEntersVehicleEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtFleetStrategyWaitingTimePercentile.class);
	
	private final FleetSpecification fleetSpecification;

	private final OptDrtConfigGroup optDrtConfigGroup;
	
    private final Config config;
	
    private int currentIteration;
    
	private int vehicleCounter = 0;
	
    private Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
    private List<Double> waitingTimes = new ArrayList<>();
	
	public OptDrtFleetStrategyWaitingTimePercentile(FleetSpecification fleetSpecification, Config config, OptDrtConfigGroup optDrtConfigGroup) {
		this.fleetSpecification = fleetSpecification;
		this.config = config;
		this.optDrtConfigGroup = optDrtConfigGroup;
	}

	@Override
	public void reset(int iteration) {		
		drtUserDepartureTime.clear();
    	waitingTimes.clear();
    	
    	this.currentIteration = iteration;
    	
    	// do not reset vehicle counter
	}

	@Override
	public void updateFleet() {
		
		if (checkIfFleetNeedsToBeIncreased()) {
			increaseFleet();
		} else {
			decreaseFleet();
		}
	}

	private boolean checkIfFleetNeedsToBeIncreased() {
		double waitingTimeThreshold = optDrtConfigGroup.getWaitingTimeThresholdForFleetSizeAdjustment();
		int cntAboveThreshold = 0;
		int cntBelowOrEqualsThreshold = 0;
		
		for (Double waitingTime : this.waitingTimes) {
			if (waitingTime > waitingTimeThreshold) {
				cntAboveThreshold++;
			} else {
				cntBelowOrEqualsThreshold++;
			}
		}
		
		double shareOfTripsAboveWaitingTimeThreshold = 0.;
		if ((cntAboveThreshold + cntBelowOrEqualsThreshold) > 0) {
			shareOfTripsAboveWaitingTimeThreshold = (double) cntAboveThreshold / (double) (cntAboveThreshold + cntBelowOrEqualsThreshold);
		} else {
			log.warn("No drt trips in iteration " + this.currentIteration);
			shareOfTripsAboveWaitingTimeThreshold = 0.;
		}
		
		if (shareOfTripsAboveWaitingTimeThreshold > (1 - optDrtConfigGroup.getTripShareThresholdForFleetSizeAdjustment()) ) {
			return true;
		} else {
			return false;
		}
	}

	private void decreaseFleet() {
		
		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();
		
		int vehiclesToRemoveFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
		int vehiclesToRemoveFromRelativeNumber = (int) (optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore) ;
		int vehiclesToRemove = Math.max(vehiclesToRemoveFromAbsoluteNumber, vehiclesToRemoveFromRelativeNumber);
		
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

		log.info("Dvrp vehicle fleet was decreased from " + vehiclesBefore + " to " + vehiclesAfter);
	}

	private void increaseFleet() {
		
		int vehiclesBefore = fleetSpecification.getVehicleSpecifications().size();		
		
		int vehiclesToAddFromAbsoluteNumber = optDrtConfigGroup.getFleetSizeAdjustment();
		int vehiclesToAddFromRelativeNumber = (int) (optDrtConfigGroup.getFleetSizeAdjustmentPercentage() * vehiclesBefore) ;
		int vehiclesToAdd = Math.max(vehiclesToAddFromAbsoluteNumber, vehiclesToAddFromRelativeNumber);
		
		log.info("Adding " + vehiclesToAdd + " vehicles...");
		
		for (int i = 0; i < vehiclesToAdd; i++) {
			
			// select a random fleet specification to be cloned.
			DvrpVehicleSpecification dvrpVehicleSpecficationToBeCloned = getRandomVehicleSpecification();
			
			if (dvrpVehicleSpecficationToBeCloned == null) {
				throw new RuntimeException("No dvrp vehicle found to be cloned. Maybe create some default dvrp vehicle which is specified somewhere. Aborting...");
			}
			
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
	public void writeInfo() {
		String runOutputDirectory = this.config.controler().getOutputDirectory();
		if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");
		
		String fileName = runOutputDirectory + "ITERS/it." + currentIteration + "/" + this.config.controler().getRunId() + "." + currentIteration + ".info_" + this.getClass().getName() + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));
			
			double waitingTimeThreshold = optDrtConfigGroup.getWaitingTimeThresholdForFleetSizeAdjustment();
			int cntAboveThreshold = 0;
			int cntBelowOrEqualsThreshold = 0;
			
			for (Double waitingTime : this.waitingTimes) {
				if (waitingTime > waitingTimeThreshold) {
					cntAboveThreshold++;
				} else {
					cntBelowOrEqualsThreshold++;
				}
			}
			
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

}

