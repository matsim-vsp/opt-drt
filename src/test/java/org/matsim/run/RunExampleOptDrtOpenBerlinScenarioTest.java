/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.core.controler.Controler;
import org.matsim.run.berlin.RunExampleOptDrtOpenBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunExampleOptDrtOpenBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunExampleOptDrtOpenBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test0() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct-drt.config.xml";
			final String[] args = {configFilename,
					"--config:controler.runId", "test0",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";
		
	        Controler controler = new RunExampleOptDrtOpenBerlinScenario().run(args, drtVehiclesFile, drtServiceAreaShpFile) ;
	        
			Assert.assertEquals("Wrong score.", -0.12345, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

			log.info( "Done with test0"  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
}
