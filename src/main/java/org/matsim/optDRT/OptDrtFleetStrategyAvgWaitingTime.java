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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;

/**
 * @author ikaddoura
 */

class OptDrtFleetStrategyAvgWaitingTime
		implements OptDrtFleetStrategy, PersonEntersVehicleEventHandler, PersonDepartureEventHandler,
		PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtFleetStrategyAvgWaitingTime.class);

	private final FleetSpecification fleetSpecification;

	private final OptDrtConfigGroup optDrtConfigGroup;

	private final Scenario scenario;

	private final OptDrtFleetModifier fleetModifier;

	private final Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
	private final Map<Integer, List<Double>> timeBin2waitingTimes = new HashMap<>();

	public OptDrtFleetStrategyAvgWaitingTime(FleetSpecification fleetSpecification, OptDrtConfigGroup optDrtConfigGroup,
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
		drtUserDepartureTime.clear();
		timeBin2waitingTimes.clear();
	}

	@Override
	public void updateFleet( int currentIteration ) {

		if (computeMaximumOfAvgWaitingTimePerTimeBin() >= optDrtConfigGroup.getWaitingTimeThresholdForFleetSizeAdjustment()) {
			fleetModifier.increaseFleet(optDrtConfigGroup.getFleetSizeAdjustment());
		} else {
			fleetModifier.decreaseFleet(optDrtConfigGroup.getFleetSizeAdjustment());
		}
		OptDrtUtils.writeModifiedFleet(fleetSpecification, scenario.getConfig(), currentIteration, this.optDrtConfigGroup.getMode());
	}

	private double computeMaximumOfAvgWaitingTimePerTimeBin() {
		double maximumPerTimeBin = 0.;
		for (Integer timeBin : this.timeBin2waitingTimes.keySet()) {
			int counter = 0;
			double sum = 0.;
			for (Double waitingTime : timeBin2waitingTimes.get(timeBin)) {
				sum = sum + waitingTime;
				counter++;
			}
			double avgWaitingTimePerTimeBin = sum / counter;
			if (avgWaitingTimePerTimeBin > maximumPerTimeBin) maximumPerTimeBin = avgWaitingTimePerTimeBin;
		}
		return maximumPerTimeBin;
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

	private int getTimeBin(double time) {
		int timeBin = (int) (time / optDrtConfigGroup.getFleetSizeTimeBinSize());
		return timeBin;
	}

	@Override
	public void writeInfo( int currentIteration ) {
		String runOutputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
		if (!runOutputDirectory.endsWith("/"))
			runOutputDirectory = runOutputDirectory.concat("/");

		String fileName = runOutputDirectory + "ITERS/it." + currentIteration + "/" + this.scenario.getConfig()
				.controler()
				.getRunId() + "." + currentIteration + ".info_" + this.getClass().getName() + "_" + this.optDrtConfigGroup.getMode() + ".csv";
		File file = new File(fileName);

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(file));

			bw.write(
					"time bin;time bin start time [sec];time bin end time [sec];average waiting time [sec];maximum waiting time[sec]");
			bw.newLine();

			for (Integer timeBin : this.timeBin2waitingTimes.keySet()) {
				int counter = 0;
				double sum = 0.;
				double maxWaitingTime = 0.;
				for (Double waitingTime : timeBin2waitingTimes.get(timeBin)) {
					sum = sum + waitingTime;
					counter++;

					if (waitingTime > maxWaitingTime) {
						maxWaitingTime = waitingTime;
					}
				}

				double timeBinStart = timeBin * optDrtConfigGroup.getFleetSizeTimeBinSize();
				double timeBinEnd = timeBin * optDrtConfigGroup.getFleetSizeTimeBinSize() + optDrtConfigGroup.getFleetSizeTimeBinSize();

				bw.write(String.valueOf(timeBin) + ";" + timeBinStart + ";" + timeBinEnd + ";" + sum / counter + ";" + maxWaitingTime );
				bw.newLine();
			}
			log.info("Output written to " + fileName);
			bw.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

