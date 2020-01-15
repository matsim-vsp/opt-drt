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
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
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
			
			String configFilename = "test/input/berlin-drt-v5.5-1pct.config_optDRT-fare.xml";
			final String[] args = {configFilename,
					"--config:global.numberOfThreads", "1",
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.0",
					"--config:plans.inputPlansFile", "one-drt-agent-inside-berlin.xml",
					"--config:controler.runId", "testFareStrategy",
					"--config:controler.lastIteration", "1",
					"--config:transit.usingTransitInMobsim", "false",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};

	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
	        Assert.assertEquals("Wrong number of drt legs in first iteration.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("In iteration 0 the fare should be very low yielding a 'normal' score.", true, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0) > 0.0);
	        
			Assert.assertEquals("Wrong number of drt legs in final iteration.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(1), MatsimTestUtils.EPSILON);
			Assert.assertEquals("In iteration 1 the fare should have increased to 9999999.0 yielding a very low score.", true, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(1) < -100000.0);

			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testFleetStrategy1() {
		try {
			
			String configFilename = "test/input/berlin-drt-v5.5-1pct.config_optDRT-fleetSize1.xml";
			final String[] args = {configFilename,
					"--config:global.numberOfThreads", "1",
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testFleetStrategy1",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-drt-agent-inside-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:transit.usingTransitInMobsim", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;

			Assert.assertEquals("Wrong number of drt legs in first iteration.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong number of drt legs in final iteration. The vehicle fleet should have been reduced to 1 vehicle.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(1), MatsimTestUtils.EPSILON);
			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void testFleetStrategy2() {
		try {
			
			String configFilename = "test/input/berlin-drt-v5.5-1pct.config_optDRT-fleetSize2.xml";
			final String[] args = {configFilename,
					"--config:global.numberOfThreads", "1",
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testFleetStrategy2",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-drt-agent-inside-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:transit.usingTransitInMobsim", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;

			Assert.assertEquals("Wrong number of drt legs in first iteration.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(0), MatsimTestUtils.EPSILON);
			Assert.assertEquals("Wrong number of drt legs in final iteration. The vehicle fleet should not have been reduced to 1 vehicle.", 2, modeAnalyzer.getIt2enteredDrtPassengers().get(1), MatsimTestUtils.EPSILON);
			
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
			
			String configFilename = "test/input/berlin-drt-v5.5-1pct.config_optDRT-area.xml";
			final String[] args = {configFilename,
					"--config:global.numberOfThreads", "1",
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testAreaStrategy1",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-drt-agent-inside-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:transit.usingTransitInMobsim", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
	        
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
			Assert.assertEquals("Wrong number of drt legs in iteration 1. The service area should have been reduced until service shutdown.", 0, drtTrips1 );
			
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
			
			String configFilename = "test/input/berlin-drt-v5.5-1pct.config_optDRT-area.xml";
			final String[] args = {configFilename,
					"--config:global.numberOfThreads", "1",
					"--config:strategy.fractionOfIterationsToDisableInnovation", "0.",
					"--config:controler.runId", "testAreaStrategy2",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "one-drt-agent-inside-berlin.xml",
					"--config:transit.useTransit", "false",
					"--config:transit.usingTransitInMobsim", "false",
					"--config:optDrt.serviceAreaAdjustmentDemandThreshold", "1",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
		
	        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
	        
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
			Assert.assertEquals("Wrong number of drt legs in iteration 1. The service area should NOT (!) have been reduced.", 2, drtTrips1 );
			
			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
}
