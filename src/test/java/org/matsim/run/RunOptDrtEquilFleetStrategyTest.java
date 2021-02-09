package org.matsim.run;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.optDRT.OptDrt;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunOptDrtEquilFleetStrategyTest {
		
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void testFleetStrategy() throws FileNotFoundException, IOException {
		Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategy.xml",
				new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
		config.controler().setRunId("testFleetStrategy");
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		// drt module
		DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
				config.plansCalcRoute());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		Controler controler = new Controler(scenario);		
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));				

		// drt-opt module
		OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));
		
		controler.run();
		
		Assert.assertEquals("Wrong score.", -74.31969437897558, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

		{
			List<List<String>> records = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(controler.getConfig().controler().getOutputDirectory() + "testFleetStrategy.drt_vehicle_stats_drt1.csv"))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			        String[] values = line.split(";");
			        records.add(Arrays.asList(values));
			    }
			}
			Assert.assertEquals("Wrong number of drt1 vehicles in first iteration:", 1., Double.valueOf(records.get(1).get(2)), MatsimTestUtils.EPSILON);		
			Assert.assertEquals("Wrong number of drt1 vehicles in last iteration:", 11., Double.valueOf(records.get(12).get(2)), MatsimTestUtils.EPSILON);
		}
		
		{
			List<List<String>> records = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(controler.getConfig().controler().getOutputDirectory() + "testFleetStrategy.drt_vehicle_stats_drt.csv"))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			        String[] values = line.split(";");
			        records.add(Arrays.asList(values));
			    }
			}
			Assert.assertEquals("Wrong number of drt vehicles in first iteration:", 1., Double.valueOf(records.get(1).get(2)), MatsimTestUtils.EPSILON);		
			Assert.assertEquals("Wrong number of drt vehicles in last iteration:", 1., Double.valueOf(records.get(12).get(2)), MatsimTestUtils.EPSILON);
		}
	}	
	
	@Test
	public final void testFleetStrategyProportional() throws FileNotFoundException, IOException {
		Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategyProportional.xml",
				new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
		config.controler().setRunId("testFleetStrategy");
		config.controler().setOutputDirectory(utils.getOutputDirectory());

		// drt module
		DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
				config.plansCalcRoute());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		Controler controler = new Controler(scenario);		
		controler.addOverridingModule(new MultiModeDrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

		// drt-opt module
		OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));
		
		controler.run();
		
//		Assert.assertEquals("Wrong score.", -74.31969437897558, controler.getScoreStats().getScoreHistory().get(ScoreItem.executed).get(0), MatsimTestUtils.EPSILON);

		{
			List<List<String>> records = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(controler.getConfig().controler().getOutputDirectory() + "testFleetStrategy.drt_vehicle_stats_drt1.csv"))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			        String[] values = line.split(";");
			        records.add(Arrays.asList(values));
			    }
			}
			Assert.assertEquals("Wrong number of drt1 vehicles in first iteration:", 1., Double.valueOf(records.get(1).get(2)), MatsimTestUtils.EPSILON);		
			Assert.assertEquals("Wrong number of drt1 vehicles in last iteration:", 3., Double.valueOf(records.get(12).get(2)), MatsimTestUtils.EPSILON);
		}
		
		{
			List<List<String>> records = new ArrayList<>();
			try (BufferedReader br = new BufferedReader(new FileReader(controler.getConfig().controler().getOutputDirectory() + "testFleetStrategy.drt_vehicle_stats_drt.csv"))) {
			    String line;
			    while ((line = br.readLine()) != null) {
			        String[] values = line.split(";");
			        records.add(Arrays.asList(values));
			    }
			}
			Assert.assertEquals("Wrong number of drt vehicles in first iteration:", 1., Double.valueOf(records.get(1).get(2)), MatsimTestUtils.EPSILON);		
			Assert.assertEquals("Wrong number of drt vehicles in last iteration:", 1., Double.valueOf(records.get(12).get(2)), MatsimTestUtils.EPSILON);
		}
	}

}
