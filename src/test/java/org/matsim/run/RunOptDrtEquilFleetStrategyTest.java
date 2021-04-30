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
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
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


    @Test
    public final void uShapedNetwork() {

        // Generate drt fleet
        String vehFileName = "drtVehicles-UShapedTest.xml";
        {
            List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
            int capacity = 1;
            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt-left-1", DvrpVehicle.class))
                    .startLinkId(Id.createLinkId("L-0-100"))
                    .capacity(capacity)
                    .serviceBeginTime(1.)
                    .serviceEndTime(108000.)
                    .build());

            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt-left-2", DvrpVehicle.class))
                    .startLinkId(Id.createLinkId("L-0-100"))
                    .capacity(capacity)
                    .serviceBeginTime(1.)
                    .serviceEndTime(108000.)
                    .build());

            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt-right-1", DvrpVehicle.class))
                    .startLinkId(Id.createLinkId("R-0-100"))
                    .capacity(capacity)
                    .serviceBeginTime(1.)
                    .serviceEndTime(108000.)
                    .build());

            vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                    .id(Id.create("drt-right-2", DvrpVehicle.class))
                    .startLinkId(Id.createLinkId("R-0-100"))
                    .capacity(capacity)
                    .serviceBeginTime(1.)
                    .serviceEndTime(108000.)
                    .build());


            new FleetWriter(vehicles.stream()).write("./test/input/equil/" + vehFileName);
        }

        // setup config
        Config config = ConfigUtils.loadConfig("test/input/equil/config-with-drt-fleetStrategy.xml",
                new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
        config.controler().setRunId("testFleetStrategy");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.network().setInputFile(null);
        config.plans().setInputFile(null);

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

        drtConfigGroupDrt.setVehiclesFile("./" + vehFileName);


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
        Scenario scenario = ScenarioUtils.createScenario(config);

        // Create Network
        Map<String, Node> nodeMap = new HashMap<>();
        {
            Network network = scenario.getNetwork();
            NetworkFactory nf = network.getFactory();

            // Create Nodes
            int xCoordLeft = 0;
            int xCoordRight = 100 * 1000; // 100 km
            for (int yCoord : List.of(0, 100, 200)) {
                String nodeIdLeft = "L-" + yCoord;
                Node nodeLeft = nf.createNode(Id.createNodeId(nodeIdLeft), new Coord(xCoordLeft, yCoord));
                nodeMap.put(nodeIdLeft, nodeLeft);
                network.addNode(nodeLeft);

                String nodeIdRight = "R-" + yCoord;
                Node nodeRight = nf.createNode(Id.createNodeId(nodeIdRight), new Coord(xCoordRight, yCoord));
                nodeMap.put(nodeIdRight, nodeRight);
                network.addNode(nodeRight);

            }

            // Add all vertical links to network
            for (List<Integer> yCoords : List.of(List.of(0, 100), List.of(100, 200))) {
                for (String leftOrRight : List.of("L", "R")) {
                    int yCoordLower = yCoords.get(0);
                    int yCoordHigher = yCoords.get(1);

                    Id<Link> linkUpwardId = Id.createLinkId(leftOrRight + "-" + yCoordLower + "-" + yCoordHigher);
                    Id<Link> linkDownwardId = Id.createLinkId(leftOrRight + "-" + yCoordHigher + "-" + yCoordLower);

                    Link linkUpward = nf.createLink(linkUpwardId, nodeMap.get(leftOrRight + "-" + yCoordLower), nodeMap.get(leftOrRight + "-" + yCoordHigher));
                    Link linkDownward = nf.createLink(linkDownwardId, nodeMap.get(leftOrRight + "-" + yCoordHigher), nodeMap.get(leftOrRight + "-" + yCoordLower));

                    network.addLink(linkUpward);
                    network.addLink(linkDownward);

                }
            }

            // Add horizontal links to network
            Id<Link> connectorRightwardId = Id.createLinkId("connector-rightward");
            Id<Link> connectorLeftwardId = Id.createLinkId("connector-leftward");

            Link connectorRightward = nf.createLink(connectorRightwardId, nodeMap.get("L-0"), nodeMap.get("R-0"));
            Link connectorLeftward = nf.createLink(connectorLeftwardId, nodeMap.get("R-0"), nodeMap.get("L-0"));

            network.addLink(connectorRightward);
            network.addLink(connectorLeftward);
        }

        // Create Population
        {
            Population population = scenario.getPopulation();
            PopulationFactory pf = population.getFactory();

            population.addPerson(generatePerson("1", "L-100-0", "L-200-100", 6. * 3600, pf));
            population.addPerson(generatePerson("2", "L-0-100", "L-200-100", 7. * 3600, pf));
            population.addPerson(generatePerson("3", "L-200-100", "L-0-100", 8. * 3600, pf));
            population.addPerson(generatePerson("4", "L-100-200", "L-0-100", 9. * 3600, pf));
            population.addPerson(generatePerson("5", "L-100-0", "L-100-200", 10. * 3600, pf));
            population.addPerson(generatePerson("6", "L-100-0", "L-200-100", 11. * 3600, pf));
            population.addPerson(generatePerson("7", "L-0-100", "L-100-200", 12. * 3600, pf));

        }
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());


        Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));

        // drt-opt module
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));


//        controler.run();

//        FleetSpecification fleet = new FleetSpecificationImpl();

//        new FleetReader(fleet).readFile(utils.getOutputDirectory() + "testFleetStrategy.drt__vehicles.xml.gz");

//        Map<Id<DvrpVehicle>, DvrpVehicleSpecification> vehicleSpecifications = fleet.getVehicleSpecifications();
//        Assert.assertEquals("There should be exactly one vehicle", 1, vehicleSpecifications.size());
//        Assert.assertTrue("That vehicle should be drt-good", vehicleSpecifications.containsKey(Id.create("drt-good", DvrpVehicle.class)));




    }

    private Person generatePerson(String PersonId, String startLink, String endLink, Double time, PopulationFactory pf) {
        Person person = pf.createPerson(Id.createPersonId(PersonId));
        Plan plan = pf.createPlan();

        Activity h = pf.createActivityFromLinkId("h", Id.createLinkId(startLink));
        h.setEndTime(time);
        plan.addActivity(h);

        plan.addLeg(pf.createLeg("drt"));

        Activity w = pf.createActivityFromLinkId("w", Id.createLinkId(endLink));
        plan.addActivity(w);

        person.addPlan(plan);
        person.setSelectedPlan(plan);

        return person;
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
