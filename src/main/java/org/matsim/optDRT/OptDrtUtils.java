/* *********************************************************************** *
 * project: org.matsim.*
 * Controler.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetWriter;
import org.matsim.core.config.Config;

class OptDrtUtils {

	static final void writeModifiedFleet(FleetSpecification fleetSpecification, Config config, int iteration, String mode){
		String runOutputDirectory = config.controler().getOutputDirectory();
		if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");
		String fleetFileName = runOutputDirectory + "ITERS/it." + iteration + "/" + config.controler().getRunId() + "." + iteration + "." + mode + "_vehicles.xml.gz";
		FleetWriter writer = new FleetWriter(fleetSpecification.getVehicleSpecifications().values().stream());
		writer.write(fleetFileName);
	}

}
