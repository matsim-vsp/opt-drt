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

/**
 *
 */
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.optDRT.OptDrtConfigGroup.FareUpdateApproach;

/**
 * @author ikaddoura
 *
 * An implementation for different DRT fares for different times of day.
 * The fares will be updated during the simulation depending on the drt users' waiting times.
 *
 * Note that these fares are scored in excess to anything set in the modeparams in the config file or any other drt fare handler.
 */
class OptDrtFareStrategyWaitingTimePercentile
		implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonArrivalEventHandler,
		OptDrtFareStrategy, DrtRequestSubmittedEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtFareStrategyWaitingTimePercentile.class);

	private final Map<Integer, Double> timeBin2distanceFarePerMeter = new HashMap<>();

	private final Map<Id<Person>, DrtRequestSubmittedEvent> lastRequestSubmission = new HashMap<>();
	private final Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
	private final Map<Integer, List<Double>> timeBin2waitingTimes = new HashMap<>();

	private int currentIteration;
	private int priceUpdateCounter;

	private final OptDrtConfigGroup optDrtConfigGroup;

	private final EventsManager events;

	private final Scenario scenario;

	public OptDrtFareStrategyWaitingTimePercentile(OptDrtConfigGroup optDrtConfigGroup, EventsManager events,
			Scenario scenario) {
		this.optDrtConfigGroup = optDrtConfigGroup;
		this.events = events;
		this.scenario = scenario;
	}

	@Override
	public void reset(int iteration) {

		lastRequestSubmission.clear();
		drtUserDepartureTime.clear();
		timeBin2waitingTimes.clear();

		this.currentIteration = iteration;

		// do not reset the fares from one iteration to the next one
	}

    @Override
    public void handleEvent(PersonArrivalEvent event) {
    	
        if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {
        	
            DrtRequestSubmittedEvent e = this.lastRequestSubmission.get(event.getPersonId());

            int timeBin = getTimeBin(drtUserDepartureTime.get(event.getPersonId()));
			double timeBinDistanceFare = 0;
			if (this.timeBin2distanceFarePerMeter.get(timeBin) != null) {
				timeBinDistanceFare = this.timeBin2distanceFarePerMeter.get(timeBin);
			}
			double fare = e.getUnsharedRideDistance() * timeBinDistanceFare;
            events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare));
        
			this.drtUserDepartureTime.remove(event.getPersonId());
        }

    }

	private int getTimeBin(double time) {
		int timeBin = (int) (time / optDrtConfigGroup.getFareTimeBinSize());
		return timeBin;
	}

	@Override
	public void updateFares() {
		
		priceUpdateCounter++;
						
		for (int timeBin = 0; timeBin <= getTimeBin(scenario.getConfig().qsim().getEndTime()); timeBin ++) {
			
			boolean increaseFare = false;
			
			if (timeBin2waitingTimes.get(timeBin) != null) {			
				
				int cntAboveThreshold = 0;
				int cntBelowOrEqualsThreshold = 0;
				
				for (Double waitingTime : this.timeBin2waitingTimes.get(timeBin)) {
					if (waitingTime > optDrtConfigGroup.getWaitingTimeThresholdForFareAdjustment()) {
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
				
				if (shareOfTripsAboveWaitingTimeThreshold > (1 - optDrtConfigGroup.getTripShareThresholdForFareAdjustment()) ) {
					increaseFare = true;
				}			
			}
						
			double oldDistanceFare = 0.;		
			if (timeBin2distanceFarePerMeter.get(timeBin) != null) {
				oldDistanceFare = timeBin2distanceFarePerMeter.get(timeBin);
			}

			double updatedDistanceFare = 0.;
			
			if (optDrtConfigGroup.getFareUpdateApproach() == FareUpdateApproach.BangBang) {
				if (increaseFare) {
					updatedDistanceFare = oldDistanceFare + optDrtConfigGroup.getFareAdjustment();
				} else {
					updatedDistanceFare = oldDistanceFare - optDrtConfigGroup.getFareAdjustment();
				}
				
			} else if (optDrtConfigGroup.getFareUpdateApproach() == FareUpdateApproach.BangBangWithMSA) {
				
				if (increaseFare) {
					updatedDistanceFare = oldDistanceFare + optDrtConfigGroup.getFareAdjustment();
				} else {
					updatedDistanceFare = oldDistanceFare - optDrtConfigGroup.getFareAdjustment();
				}
				
				double blendFactor = (1 / (double) priceUpdateCounter);
				updatedDistanceFare = (1 - blendFactor) * oldDistanceFare + blendFactor * updatedDistanceFare;
				
			} else if (optDrtConfigGroup.getFareUpdateApproach() == FareUpdateApproach.Proportional) {
				throw new RuntimeException("Not implemented. Aborting...");
			
			} else if (optDrtConfigGroup.getFareUpdateApproach() == FareUpdateApproach.ProportionalWithMSA) {
				throw new RuntimeException("Not implemented. Aborting...");
				
			} else if (optDrtConfigGroup.getFareUpdateApproach() == FareUpdateApproach.SimpleOffset) {
				if (increaseFare) {
					updatedDistanceFare = optDrtConfigGroup.getFareAdjustment();
				} else {
					updatedDistanceFare = 0.;
				}
			}

			// do not allow for negative fares
			if (updatedDistanceFare < 0.) updatedDistanceFare = 0.;
			
			log.info("Fare in time bin " + timeBin + " changed from " + oldDistanceFare + " to " + updatedDistanceFare);
			
			timeBin2distanceFarePerMeter.put(timeBin, updatedDistanceFare);
		}
	}

	@Override
	public void handleEvent(DrtRequestSubmittedEvent event) {
		 if (optDrtConfigGroup.getMode().equals(event.getMode())) {
			 this.lastRequestSubmission.put(event.getPersonId(), event);
	     }
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {
			this.drtUserDepartureTime.put(event.getPersonId(), event.getTime());
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		
		if (this.drtUserDepartureTime.get(event.getPersonId()) != null) {
			
			double waitingTime = event.getTime() - this.drtUserDepartureTime.get(event.getPersonId());
			int timeBin = getTimeBin(event.getTime());
			
			if (this.timeBin2waitingTimes.get(timeBin) == null) {
				List<Double> waitingTimes = new ArrayList<>();
				waitingTimes.add(waitingTime);
				this.timeBin2waitingTimes.put(timeBin, waitingTimes);
			} else {
				this.timeBin2waitingTimes.get(timeBin).add(waitingTime);
			}
			
		}
	}

	@Override
	public void writeInfo() {
		String runOutputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");
		
		String fileName = runOutputDirectory + "ITERS/it." + currentIteration + "/" + this.scenario.getConfig().controler().getRunId() + "." + currentIteration + ".info_" + this.getClass().getName() + ".csv";
		File file = new File(fileName);			

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write("time bin;time bin start time [sec];time bin end time [sec];average waiting time [sec];maximum waiting time[sec];fare [monetary units / meter]");
			bw.newLine();

			for (Integer timeBin : this.timeBin2distanceFarePerMeter.keySet()) {
				int counter = 0;
				double sum = 0.;
				double maxWaitingTime = 0.;
				if (timeBin2waitingTimes.get(timeBin) != null) {
					for (Double waitingTime : timeBin2waitingTimes.get(timeBin)) {
						sum = sum + waitingTime;
						counter++;
						
						if (waitingTime > maxWaitingTime) {
							maxWaitingTime = waitingTime;
						}
					}
				}
				
				double timeBinStart = timeBin * optDrtConfigGroup.getFareTimeBinSize();
				double timeBinEnd = timeBin * optDrtConfigGroup.getFareTimeBinSize() + optDrtConfigGroup.getFareTimeBinSize();
			
				double fare = 0.;
				if (this.timeBin2distanceFarePerMeter.get(timeBin) != null) fare = this.timeBin2distanceFarePerMeter.get(timeBin);
				
				bw.write(String.valueOf(timeBin) + ";" + timeBinStart + ";" + timeBinEnd + ";" + sum / counter + ";" + maxWaitingTime + ";" + String.valueOf(fare) );
				bw.newLine();
			}
			log.info("Output written to " + fileName);
			bw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
