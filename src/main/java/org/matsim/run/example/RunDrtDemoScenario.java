package org.matsim.run.example;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFareModule;
import org.matsim.contrib.av.robotaxi.fares.taxi.TaxiFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.optDRT.OptDrtModule;
import org.matsim.run.RunBerlinScenario;

/**
 * In this class a demo scenario with taxi and drt will be tested.
 *
 * @author zmeng
 */
public class RunDrtDemoScenario {
    private static final Logger log = Logger.getLogger(RunDrtDemoScenario.class);
    private RunBerlinScenario demo;

    private RunDrtDemoScenario(String[] args) {
        String configFileName;

        if ( args.length != 0 ){
            configFileName = args[0];
        } else {
            configFileName = "path-to-config-file"; // start from matsim-berlin 5.3 version and extend by drt-required stuff
        }

        this.demo = new RunBerlinScenario(configFileName, null);
    }

    public static void main(String[] args) {
        new RunDrtDemoScenario(args).run() ;
    }

    private void run() {
        // dvrp, drt, taxi, optDrt config groups
        ConfigGroup[] configGroups = {new DvrpConfigGroup(), new DrtConfigGroup(), new TaxiConfigGroup(),
                new DrtFaresConfigGroup(), new TaxiFaresConfigGroup(), new OptDrtConfigGroup() };
        Config config = demo.prepareConfig(configGroups);

        DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
        //todo: check if we have stage Activities for both drt and taxi

        Scenario scenario = demo.prepareScenario();
        RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
        routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        // todo: try to find a way to get rid of these, because we dont need the out of service area things
        //BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
        //new NetworkModification(shpUtils).addDRTmode(scenario, drtNetworkMode, drtServiceAreaAttribute);
        //new PersonAttributesModification(shpUtils, stageActivities).run(scenario);

        Controler controler = demo.prepareControler();

        // drt + taxi + dvrp module
        controler.addOverridingModule(new TaxiModule());
        controler.addOverridingModule(new DrtModule());
        controler.addOverridingModule(new DvrpModule());
        controler.configureQSimComponents(
                DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));
        controler.configureQSimComponents(
                DvrpQSimComponents.activateModes(TaxiConfigGroup.get(controler.getConfig()).getMode()));

        // Add drt & taxi-specific fare module
        controler.addOverridingModule(new DrtFareModule());
        controler.addOverridingModule(new TaxiFareModule());

        // Add optDrt module
        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
        controler.addOverridingModule(new OptDrtModule(optDrtConfigGroup));

        controler.run();

        log.info("Done.");
    }

}
