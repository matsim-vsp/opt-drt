package org.matsim.run;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.fleet.*;
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
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunOptDrtEquilFleetStrategyTest {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void testFleetStrategy() throws IOException {
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

    @Test
    public final void testFleetStrategyWeightedRandomVehicleSelection() throws FileNotFoundException, IOException {
        Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategy.xml",
                new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
        config.controler().setRunId("testFleetStrategy");
        config.controler().setOutputDirectory(utils.getOutputDirectory());

        MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
        OptDrtConfigGroup optDrtConfigGroupDrt = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        optDrtConfigGroupDrt.setFleetUpdateVehicleSelection(OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration);
        optDrtConfigGroupDrt.setVehicleSelectionRandomnessConstant(0.0);

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroupDrt = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        DrtSpeedUpParams speedUpParams = new DrtSpeedUpParams();
        speedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
        speedUpParams.setFractionOfIterationsSwitchOff(0.9);
        speedUpParams.setFractionOfIterationsSwitchOn(0.0);
        speedUpParams.setIntervalDetailedIteration(5);
        drtConfigGroupDrt.addParameterSet(speedUpParams);

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
    public final void testFleetStrategyWeightedRandomVehicleSelection_increaseFleetSize()  {
        Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategy.xml",
                new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
        config.controler().setRunId("testFleetStrategy");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.plans().setInputFile(null);

        // Create DRT Vehicle Fleet
        List<DvrpVehicleSpecification> vehicles = createFleet();

        new FleetWriter(vehicles.stream()).write("./test/input/equil/drtVehicles-FleetSizeTest.xml");

        // opt
        MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
        OptDrtConfigGroup optDrtConfigGroupDrt = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        optDrtConfigGroupDrt.setFleetUpdateVehicleSelection(OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration);
        optDrtConfigGroupDrt.setVehicleSelectionRandomnessConstant(0.0);

        optDrtConfigGroupDrt.setWaitingTimeThresholdForFleetSizeAdjustment(0.);//jr

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroupDrt = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        DrtSpeedUpParams speedUpParams = new DrtSpeedUpParams();
        speedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
        speedUpParams.setFractionOfIterationsSwitchOff(0.9);
        speedUpParams.setFractionOfIterationsSwitchOn(0.0);
        speedUpParams.setIntervalDetailedIteration(5);
        drtConfigGroupDrt.addParameterSet(speedUpParams);

        drtConfigGroupDrt.setVehiclesFile("./drtVehicles-FleetSizeTest.xml");


        //remove drt1
        DrtConfigGroup drtConfigGroupDrt1 = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeDrtConfigGroup.removeParameterSet(drtConfigGroupDrt1);

        OptDrtConfigGroup optDrtConfigGroupDrt1 = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeOptDrtConfigGroup.removeParameterSet(optDrtConfigGroupDrt1);

        String[] modes = new String[]{"car", "drt"};
        config.subtourModeChoice().setModes(modes);


        // drt module
        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
                config.plansCalcRoute());


        // Create Scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Add population of one
        AddPopulationOfOne(scenario);

        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());


        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

        // drt-opt module
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));


        controler.run();

        FleetSpecification fleet = new FleetSpecificationImpl();

        new FleetReader(fleet).readFile(utils.getOutputDirectory() + "testFleetStrategy.drt__vehicles.xml.gz");

        Map<Id<DvrpVehicle>, DvrpVehicleSpecification> vehicleSpecifications = fleet.getVehicleSpecifications();

        for (DvrpVehicleSpecification veh : vehicleSpecifications.values()) {
            String id = veh.getId().toString();
            if (id.contains("optDrt")) {
                Assert.assertTrue("Every vehicle created by optDrt should be a clone of vehicle drt-good",
                        id.contains("cloneOf_drt-good"));
            }
        }
    }

    private List<DvrpVehicleSpecification> createFleet() {
        List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
        int capacity = 1;

        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt-good", DvrpVehicle.class))
                .startLinkId(Id.createLinkId(1))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(60)
                .build());

        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder().id(Id.create("drt-bad", DvrpVehicle.class))
                .startLinkId(Id.createLinkId(2))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(108000.)
                .build());
        return vehicles;
    }

    @Test
    public final void testFleetStrategyWeightedRandomVehicleSelection_decreaseFleetSize() {
        Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategy.xml",
                new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
        config.controler().setRunId("testFleetStrategy");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.plans().setInputFile(null);

        // Create DRT Vehicle Fleet
        List<DvrpVehicleSpecification> vehicles = createFleet();

        new FleetWriter(vehicles.stream()).write("./test/input/equil/drtVehicles-FleetSizeTest.xml");

        // opt
        MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
        OptDrtConfigGroup optDrtConfigGroupDrt = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        optDrtConfigGroupDrt.setFleetUpdateVehicleSelection(OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration);
        optDrtConfigGroupDrt.setVehicleSelectionRandomnessConstant(0.0);

//        optDrtConfigGroupDrt.setWaitingTimeThresholdForFleetSizeAdjustment(0.);//jr

        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroupDrt = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        DrtSpeedUpParams speedUpParams = new DrtSpeedUpParams();
        speedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
        speedUpParams.setFractionOfIterationsSwitchOff(0.9);
        speedUpParams.setFractionOfIterationsSwitchOn(0.0);
        speedUpParams.setIntervalDetailedIteration(5);
        drtConfigGroupDrt.addParameterSet(speedUpParams);

        drtConfigGroupDrt.setVehiclesFile("./drtVehicles-FleetSizeTest.xml");


        //remove drt1
        DrtConfigGroup drtConfigGroupDrt1 = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeDrtConfigGroup.removeParameterSet(drtConfigGroupDrt1);

        OptDrtConfigGroup optDrtConfigGroupDrt1 = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeOptDrtConfigGroup.removeParameterSet(optDrtConfigGroupDrt1);

        String[] modes = new String[]{"car", "drt"};
        config.subtourModeChoice().setModes(modes);


        // drt module
        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
                config.plansCalcRoute());


        // Create Scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        // Add population of one
        AddPopulationOfOne(scenario);

        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());


        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

        // drt-opt module
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));


        controler.run();

        FleetSpecification fleet = new FleetSpecificationImpl();

        new FleetReader(fleet).readFile(utils.getOutputDirectory() + "testFleetStrategy.drt__vehicles.xml.gz");

        Map<Id<DvrpVehicle>, DvrpVehicleSpecification> vehicleSpecifications = fleet.getVehicleSpecifications();

        Assert.assertEquals("There should be exactly one vehicle", 1, vehicleSpecifications.size());
        Assert.assertTrue("That vehicle should be drt-good", vehicleSpecifications.containsKey(Id.create("drt-good", DvrpVehicle.class)));

    }

    private void AddPopulationOfOne(Scenario scenario) {
        Population population = scenario.getPopulation();
        PopulationFactory pf = population.getFactory();
        Person eve = pf.createPerson(Id.createPersonId("eve"));
        Plan plan = pf.createPlan();
        Activity h = pf.createActivityFromCoord("h", new Coord(-2500, 0));
        h.setLinkId(Id.createLinkId("1"));
        h.setEndTime(6. * 3600);
        plan.addActivity(h);

        plan.addLeg(pf.createLeg("drt"));

        Activity w = pf.createActivityFromCoord("w", new Coord(1000, 0));
        w.setLinkId(Id.createLinkId("20"));
        w.setEndTime(16. * 3600);
        plan.addActivity(w);

        eve.addPlan(plan);
        eve.setSelectedPlan(plan);
        population.addPerson(eve);
    }

}
