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
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.run.berlin.RunDrtOpenBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */
public class RunDrtOpenBerlinScenarioTest {
	private static final Logger log = Logger.getLogger( RunDrtOpenBerlinScenarioTest.class ) ;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test1Agent() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "test0",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "../../../test/input/one-test-agent.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
			Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
	        Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			Assert.assertEquals("Wrong number of drt legs in final iteation.", 2, modeAnalyzer.getEnteredDrtVehicles());
			
			DrtFareConfigGroup fareCfg = (DrtFareConfigGroup) config.getModules().get(DrtFareConfigGroup.GROUP_NAME);
			Assert.assertEquals("Wrong minimum fare.", 4., fareCfg.getMinFarePerTrip(), MatsimTestUtils.EPSILON);

			log.info( "Done."  );
			log.info("") ;
			
		} catch ( Exception ee ) {
			ee.printStackTrace();
			throw new RuntimeException(ee) ;
		}
	}
	
	@Test
	public final void test1AgentRejected() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "test0",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "../../../test/input/one-test-agent.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";
		
			Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
	        Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
	        
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
	public final void test1agentSameLink() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "test0",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "../../../test/input/one-test-agent-sameLink.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
			Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
	        Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
	        
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
	public final void testAnotherAgent() {
		try {
			
			String configFilename = "scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml";
			final String[] args = {configFilename,
					"--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
					"--config:controler.runId", "test1",
					"--config:controler.lastIteration", "1",
					"--config:plans.inputPlansFile", "../../../test/input/another-test-agent.xml",
					"--config:transit.useTransit", "false",
					"--config:controler.outputDirectory", utils.getOutputDirectory()};
			
			String drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
			String drtServiceAreaShpFile = null;
		
			Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
	        Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
	        
	        ModeAnalyzer modeAnalyzer = new ModeAnalyzer();
	        
	        controler.addOverridingModule(new AbstractModule() {
				
				@Override
				public void install() {
					this.addEventHandlerBinding().toInstance(modeAnalyzer);
				}
			});
	        
	        controler.run() ;
	        
			Assert.assertEquals("Wrong number of drt legs in final iteation.", 1, modeAnalyzer.getEnteredDrtVehicles());

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
		
			Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
			Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
	        Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
	        	        
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
	
}
