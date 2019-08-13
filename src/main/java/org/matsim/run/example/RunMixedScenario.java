package org.matsim.run.example;

import org.locationtech.jts.util.CollectionUtil;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;

public class RunMixedScenario {
    public static void main(String[] args){
        final String CONFIG_FILE = "scenarios/demo/demo_config.xml";

        // load config
        DrtConfigGroup drtConfigGroup = new DrtConfigGroup();
        Config config = ConfigUtils.loadConfig(CONFIG_FILE, new TaxiConfigGroup(), new DvrpConfigGroup(), drtConfigGroup);

        config.addModule(new DrtFareConfigGroup());
        config.addModule(new TaxiFareConfigGroup());


        config.controler().setLastIteration(20);
        config.qsim().setMainModes(CollectionUtils.stringToSet("car"));

        config.controler().setRoutingAlgorithmType(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks);
        config.subtourModeChoice().setProbaForRandomSingleTripMode(0.5D);
        config.plansCalcRoute().setRoutingRandomness(3.0D);
        config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
        config.plansCalcRoute().setInsertingAccessEgressWalk(false);
        config.qsim().setUsingTravelTimeCheckInTeleportation(true);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.kinematicWaves);

        config.controler().setOutputDirectory("output/example/mixed/");

        String[] modes = {"drt","taxi"};

        config.changeMode().setModes(modes);
        config.strategy().setFractionOfIterationsToDisableInnovation(0.8);

        config.controler().setWriteEventsInterval(1);
        config.controler().setWritePlansInterval(1);

        drtConfigGroup.setRequestRejection(false);



        // load scenario
        Scenario scenario = ScenarioUtils.loadScenario(config);

        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());


        // setup controler
        Controler controler = new Controler(scenario);

        String taxiMode = TaxiConfigGroup.get(config).getMode();
        String drtMode = DrtConfigGroup.get(config).getMode();

        controler.addOverridingModule(new TaxiModule());
        controler.addOverridingModule(new TaxiFareModule());

        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new DrtModule());
        controler.addOverridingModule(new DrtFareModule());


        String[] dvrpModes = {taxiMode,drtMode};
        controler.configureQSimComponents(DvrpQSimComponents.activateModes(dvrpModes));


        // run simulation
        controler.run();


    }
}

