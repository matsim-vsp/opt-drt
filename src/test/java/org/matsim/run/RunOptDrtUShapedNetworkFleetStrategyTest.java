package org.matsim.run;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.drt.analysis.zonal.DrtZonalSystemParams;
import org.matsim.contrib.drt.optimizer.rebalancing.RebalancingParams;
import org.matsim.contrib.drt.optimizer.rebalancing.mincostflow.MinCostFlowRebalancingStrategyParams;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunOptDrtUShapedNetworkFleetStrategyTest {

    /**
     *
     * The following tests are based on the following network shown below. The vertical links are short (100m)
     * while the horizontal connector is comparatively long (2000m). There are two drt vehicles placed at L-0
     * and two at R-0. There is a high demand for drt trips on the left side and no demand on the right side.
     *
     * The purpose of the tests below is to check that NetworkFleetStrategy of WeightedRandomVehicleSelection will
     * choose the correct vehicles to clone or remove. The network design is meant to ensure that the FleetStrategy
     * will always go in favor of the left vehicles, since the stay time of the right vehicles is high, since no trips
     * should assigned to them.
     *
     * 200   x                                                  x
     *      ||                                                 ||
     *      ||                                                 ||
     *      ||                                                 ||
     * 100   x                                                  x
     *      ||                                                 ||
     *      ||                                                 ||
     *      ||                                                 ||
     *  0    x ===============[id="connector"]================= x
     *                          l=2000m
     *       L                                                  R
     *
     *  Notation:
     *  nodes: represented by "x"; id: [L/R]-[0/100/200] *see x and y axis above
     *  links: represented by "||" or "="; id: [L/R]-[yCoordFrom]-[yCoordTo]
     */

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    /**
     * With a high waitingTimeThresholdForFleetSizeAdjustment, the FleetStrategy will choose to remove vehicles.
     * At the end, there should only be one remaining vehicle, and it should be one of the two left vehicles.
     */
    @Test
    public final void testWeightedRandomVehicleSelection_decreaseFleet() {

        // Generate drt fleet - two vehicles on L-0-100, two vehicles on R-0-100, each with capacity of 1
        String vehFileName = "drtVehicles-UShapedTest.xml";
        String configFileName = "test/input/equil/config-with-drt-fleetStrategy.xml";
        double waitingTimeThresholdForFleetSizeAdjustment = 999999.0;

        // setup config
        Config config = generateConfig(configFileName, vehFileName, waitingTimeThresholdForFleetSizeAdjustment);

        // Create Scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        // Generate U-Shaped Network
        generateUShapedNetwork(scenario);

        // Generate Population
        generatePopulationOnLeftSide(scenario);
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // Generate Drt Vehicle Fleet
        generateVehicleFleet(vehFileName);

        // Setup Controler
        org.matsim.core.controler.Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));

        controler.run();

        FleetSpecification fleet = new FleetSpecificationImpl();

        new FleetReader(fleet).readFile(utils.getOutputDirectory() + "testFleetStrategy.drt__vehicles.xml.gz");

        Map<Id<DvrpVehicle>, DvrpVehicleSpecification> vehicleSpecifications = fleet.getVehicleSpecifications();

        Assert.assertEquals("There should be exactly one vehicle", 1, vehicleSpecifications.size());
        fleet.getVehicleSpecifications().values().forEach(veh ->
                Assert.assertTrue(veh.getId().toString().contains("drt-left"))
        );


    }

    /**
     * With a high waitingTimeThresholdForFleetSizeAdjustment, the FleetStrategy will choose to add vehicles.
     * Since the vehicles on the left side of the network will always have shorter stay times, all clones should
     * be of the left vehicles. Therefore, all clones should have "drt-left" contained in their id.
     */

    @Test
    public final void testWeightedRandomVehicleSelection_increaseFleet() {

        // Generate drt fleet - two vehicles on L-0-100, two vehicles on R-0-100, each with capacity of 1
        String vehFileName = "drtVehicles-UShapedTest.xml";
        String configFileName = "test/input/equil/config-with-drt-fleetStrategy.xml";
        double waitingTimeThresholdForFleetSizeAdjustment = 0.;

        // setup config
        Config config = generateConfig(configFileName, vehFileName, waitingTimeThresholdForFleetSizeAdjustment);


        // Create Scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        // Generate U-Shaped Network
        generateUShapedNetwork(scenario);

        // Generate Population
        generatePopulationOnLeftSide(scenario);
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // Generate Drt Vehicle Fleet
        generateVehicleFleet(vehFileName);

        // Setup Controler
        org.matsim.core.controler.Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));

        controler.run();

        FleetSpecification fleet = new FleetSpecificationImpl();

        new FleetReader(fleet).readFile(utils.getOutputDirectory() + "testFleetStrategy.drt__vehicles.xml.gz");

        Map<Id<DvrpVehicle>, DvrpVehicleSpecification> vehicleSpecifications = fleet.getVehicleSpecifications();

        for (DvrpVehicleSpecification veh : vehicleSpecifications.values()) {
            String id = veh.getId().toString();
            if (id.contains("optDrt")) {
                Assert.assertTrue("Every vehicle created by optDrt should be a clone of vehicle drt-left",
                        id.contains("cloneOf_drt-left"));
            }
        }

    }


    /**
     * Unfinished Test to check the interaction between optDrt and Rebalancing
     * Once the desired outcomes of the combination of optDrt + rebalancing
     * are determined then ... TODO: add applicable assert statements
     */
    @Test
    public final void testWeightedRandomVehicleSelection_increaseFleet_withRebalancing() {

        // Generate drt fleet - two vehicles on L-0-100, two vehicles on R-0-100, each with capacity of 1
        String vehFileName = "drtVehicles-UShapedTest.xml";
        String configFileName = "test/input/equil/config-with-drt-fleetStrategy.xml";
        double waitingTimeThresholdForFleetSizeAdjustment = 0.;

        // setup config
        Config config = generateConfig(configFileName, vehFileName, waitingTimeThresholdForFleetSizeAdjustment);

        // Create Scenario
        Scenario scenario = ScenarioUtils.createScenario(config);

        // Generate U-Shaped Network
        generateUShapedNetwork(scenario);

        // Generate Population
        generatePopulationOnLeftSide(scenario);
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // Generate Drt Vehicle Fleet
        generateVehicleFleet(vehFileName);

        // Setup Controler
        org.matsim.core.controler.Controler controler = new Controler(scenario);
        controler.addOverridingModule(new MultiModeDrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(DvrpQSimComponents.activateAllModes(MultiModeDrtConfigGroup.get(controler.getConfig())));
        OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));

        // Add Rebalancing Strategy to drt config: minCostFlowRebalancing
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroupDrt = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();

        RebalancingParams rebalancingParams = new RebalancingParams();
        rebalancingParams.setInterval(1800);
        MinCostFlowRebalancingStrategyParams minCostFlowRebalancingStrategyParams = new MinCostFlowRebalancingStrategyParams();
        minCostFlowRebalancingStrategyParams.setTargetAlpha(1.);
        minCostFlowRebalancingStrategyParams.setTargetBeta(0.);

        minCostFlowRebalancingStrategyParams.setZonalDemandEstimatorType(MinCostFlowRebalancingStrategyParams.ZonalDemandEstimatorType.PreviousIterationDemand);

        rebalancingParams.addParameterSet(minCostFlowRebalancingStrategyParams);

        drtConfigGroupDrt.addParameterSet(rebalancingParams);

        // add zonal system
        DrtZonalSystemParams zonal = new DrtZonalSystemParams();
        zonal.setCellSize(1000.);
        zonal.setZonesGeneration(DrtZonalSystemParams.ZoneGeneration.GridFromNetwork);
        drtConfigGroupDrt.addParameterSet(zonal);


        controler.run();

        //TODO: write assert statements once desired outcomes are determined

    }

    private Config generateConfig(String configFilename, String vehFileName, double waitingTimeThresholdForFleetSizeAdjustment) {
        Config config = ConfigUtils.loadConfig(configFilename,
                new MultiModeDrtConfigGroup(), new DvrpConfigGroup(), new MultiModeOptDrtConfigGroup());
        config.controler().setRunId("testFleetStrategy");
        config.controler().setOutputDirectory(utils.getOutputDirectory());
        config.network().setInputFile(null);
        config.plans().setInputFile(null);
        config.subtourModeChoice().setModes(new String[]{"car", "drt"}); // removes drt1

        // OptDrt Configuration
        // Mode drt
        MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class);
        multiModeOptDrtConfigGroup.setUpdateInterval(4);

        OptDrtConfigGroup optDrtConfigGroupDrt = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        optDrtConfigGroupDrt.setFleetUpdateVehicleSelection(OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration);
        optDrtConfigGroupDrt.setVehicleSelectionRandomnessConstant(0.);
        optDrtConfigGroupDrt.setFleetUpdateApproach(OptDrtConfigGroup.FleetUpdateApproach.BangBang);
        optDrtConfigGroupDrt.setFleetSizeAdjustment(1);

        optDrtConfigGroupDrt.setWaitingTimeThresholdForFleetSizeAdjustment(waitingTimeThresholdForFleetSizeAdjustment);

        // Mode drt1 - remove
        OptDrtConfigGroup optDrtConfigGroupDrt1 = multiModeOptDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeOptDrtConfigGroup.removeParameterSet(optDrtConfigGroupDrt1);

        // MultiModeDrt Configuration
        // Mode drt - configure and speed up
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroupDrt = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt")).findAny().orElseThrow();
        drtConfigGroupDrt.setVehiclesFile("./" + vehFileName);
        drtConfigGroupDrt.setDrtServiceAreaShapeFile(null);
        drtConfigGroupDrt.setOperationalScheme(DrtConfigGroup.OperationalScheme.door2door);

        DrtSpeedUpParams speedUpParams = new DrtSpeedUpParams();
        speedUpParams.setFirstSimulatedDrtIterationToReplaceInitialDrtPerformanceParams(0);
        speedUpParams.setFractionOfIterationsSwitchOff(0.9);
        speedUpParams.setFractionOfIterationsSwitchOn(0.0);
        speedUpParams.setIntervalDetailedIteration(2);
        drtConfigGroupDrt.addParameterSet(speedUpParams);

        // Mode drt1 - remove
        DrtConfigGroup drtConfigGroupDrt1 = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals("drt1")).findAny().orElseThrow();
        multiModeDrtConfigGroup.removeParameterSet(drtConfigGroupDrt1);

        // drt module
        DrtConfigs.adjustMultiModeDrtConfig(MultiModeDrtConfigGroup.get(config), config.planCalcScore(),
                config.plansCalcRoute());

        return config;
    }

    private void generateVehicleFleet(String vehFileName) {
        List<DvrpVehicleSpecification> vehicles = new ArrayList<>();
        int capacity = 1;
        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(Id.create("drt-left-1", DvrpVehicle.class))
                .startLinkId(Id.createLinkId("L-0-100"))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(120. * 60) // 2 A.M.
                .build());

        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(Id.create("drt-left-2", DvrpVehicle.class))
                .startLinkId(Id.createLinkId("L-0-100"))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(120. * 60)
                .build());

        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(Id.create("drt-right-1", DvrpVehicle.class))
                .startLinkId(Id.createLinkId("R-0-100"))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(120. * 60)
                .build());

        vehicles.add(ImmutableDvrpVehicleSpecification.newBuilder()
                .id(Id.create("drt-right-2", DvrpVehicle.class))
                .startLinkId(Id.createLinkId("R-0-100"))
                .capacity(capacity)
                .serviceBeginTime(1.)
                .serviceEndTime(120. * 60)
                .build());


        new FleetWriter(vehicles.stream()).write("./test/input/equil/" + vehFileName);
    }

    private void generatePopulationOnLeftSide(Scenario scenario) {
        Population population = scenario.getPopulation();
        PopulationFactory pf = population.getFactory();

        for (int minute = 0; minute <= 120; minute += 5) {

            population.addPerson(generatePerson(Integer.toString(minute), "L-100-0", "L-200-100", minute * 60., pf));
        }
    }

    private void generateUShapedNetwork(Scenario scenario) {
        // Create Network
        Map<String, Node> nodeMap = new HashMap<>();

        Network network = scenario.getNetwork();
        NetworkFactory nf = network.getFactory();

        // Create Nodes
        int xCoordLeft = 0;
        int xCoordRight = 2 * 1000; // 10 km
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

        network.getLinks().values().forEach(x -> x.setCapacity(1000));

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


}
