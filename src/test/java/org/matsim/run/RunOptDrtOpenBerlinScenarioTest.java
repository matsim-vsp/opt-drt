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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.run.berlin.RunOptDrtOpenBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunOptDrtOpenBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunOptDrtOpenBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
		
	@Test
	public final void testFareStrategy() {
		try {
			
			String configFilename = "test/input/berlin-v5.4-1pct-optDrt-fare.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "testFareStrategy",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-test-agent.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			Assert.assertEquals("Wrong number of drt legs in final iteation.", 2, modeAnalyzer.getEnteredDrtVehicles());
			Assert.assertEquals("Wrong average executed score in iteration 0.", 116.3620694252209, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("In iteration 1 the fare should have increased to 9999999.0 yielding a very low score.", true, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1) < -100000.0);

			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testFleetStrategy() {
		try {
			
			String configFilename = "test/input/berlin-v5.4-1pct-optDrt-fleetSize.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "testFleetStrategy",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-test-agent.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			Assert.assertEquals("Wrong number of drt legs in final iteation.", 0, modeAnalyzer.getEnteredDrtVehicles());
			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testAreaStrategy1() {
		try {
			
			String configFilename = "test/input/berlin-v5.4-1pct-optDrt-area.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testAreaStrategy1",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "drt-test-agent-in-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			int drtTrips0 = modeAnalyzer.getIt2enteredDrtPassengers().get(0);
			Assert.assertEquals("Wrong number of drt legs in iteation 0.", 2, drtTrips0 );

			int drtTrips1 = modeAnalyzer.getIt2enteredDrtPassengers().get(1);
			Assert.assertEquals("Wrong number of drt legs in iteation 1. ", 0, drtTrips1 );
			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testAreaStrategy2() {
		try {
			
			String configFilename = "test/input/berlin-v5.4-1pct-optDrt-area.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testAreaStrategy2",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "drt-test-agent-in-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:optDrt.serviceAreaAdjustmentDemandThreshold", "1",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			int drtTrips0 = modeAnalyzer.getIt2enteredDrtPassengers().get(0);
			Assert.assertEquals("Wrong number of drt legs in iteration 0.", 2, drtTrips0 );

			int drtTrips1 = modeAnalyzer.getIt2enteredDrtPassengers().get(1);
			Assert.assertEquals("Wrong number of drt legs in iteration 1.", 2, drtTrips1 );
			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testBerlin1pct() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "testBerlin1pct",
					"--config:controler.lastIteration", "1",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			Assert.assertEquals("Wrong average executed score in iteration 0.", 114.7485847286583, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong number of drt legs in final iteation.", 61, modeAnalyzer.getEnteredDrtVehicles());

			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testSeveralIterations() {
		try {
			
			String configFilename = "test/input/berlin-v5.4-1pct-optDrt-fleetSize.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
					"--config:controler.runId", "testSeveralIterations",
					"--config:controler.lastIteration", "30",
					"--config:plans.inputPlansFile", "stay-home-agent.xml",
					"--config:network.inputNetworkFile", "one-link-network.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "one-drt-vehicle.xml";
			String drtServiceAreaShpFile = null;
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args, drtVehiclesFile, drtServiceAreaShpFile);
	        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(controler.getConfig(), OptDrtConfigGroup.class);
	        optDrtConfigGroup.setUpdateInterval(10);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
}
