package org.matsim.run.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
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
import org.matsim.contrib.taxi.optimizer.assignment.AssignmentTaxiOptimizerParams;
import org.matsim.contrib.taxi.run.TaxiConfigGroup;
import org.matsim.contrib.taxi.run.TaxiModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.berlin.BerlinDrtMainModeIdentifier;

/**
 * 1. Based on berlin5.4, create a demo scheme, which only includes DRT and taxi modes.
 * 2. ConfigFile: very same as the berlin 5.4 config, besides the setting of network and plans File.
 * 3. PlansFile : 2 agent
 * 4. NetworkFile: simple gridiron network
 * 5. one Drt vehicle and two Taxis are available
 * Conclusion: this scheme is designed for the test of various pricing strategies of opt-drt
 * @author zmeng
 */
public class RunDrtTaxiDemoScenario {
    private static final Logger log = Logger.getLogger(RunDrtTaxiDemoScenario.class);

    public static void main(String[] args) {
        String[] arguments;
        String drtVehiclesFile;
        String taxiVehicleFile;

        if ( args.length != 0 ){

            arguments = Arrays.copyOfRange( args, 0, args.length - 2 );
            log.info("arguments: " + arguments.toString());

            for (String arg : arguments) {
                log.info("arg: " + arg);
            }

            drtVehiclesFile = args[args.length - 3];
            log.info("drtVehiclesFile: " + drtVehiclesFile);

            taxiVehicleFile = args[args.length - 2];
            log.info("taxiVehicleFile: " + taxiVehicleFile);


        } else {
            arguments = new String[] {""};
            drtVehiclesFile = "one_drt.xml";
            taxiVehicleFile = "two_taxi.xml";

        }

        Config config = prepareConfig(arguments, drtVehiclesFile, taxiVehicleFile);
        Scenario scenario = prepareScenario(config);
        prepareControler(scenario).run();
    }

    public static Config prepareConfig(String[] args, String drtVehiclesFile, String taxiVehicleFile) {
        Config config = RunBerlinScenario.prepareConfig(args);
        config.addModule(new DvrpConfigGroup());
        config.addModule(new DrtConfigGroup());
        config.addModule(new DrtFaresConfigGroup());
        config.addModule(new TaxiConfigGroup());
        config.addModule(new TaxiFaresConfigGroup());

        // add drt and Taxi mode
        List<String> modes = new ArrayList<String>(Arrays.asList(config.subtourModeChoice().getModes()));
        modes.add(TransportMode.drt);
        modes.add(TransportMode.taxi);
        config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));

        // required by drt module
        config.qsim().setNumberOfThreads(1);
        config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
        DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());

        // add drt stage activity (per default only added in case of stop-based drt operation mode)
        PlanCalcScoreConfigGroup.ActivityParams drtActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.drt + " interaction");
        drtActivityParams.setTypicalDuration(1);
        drtActivityParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(drtActivityParams));
        config.planCalcScore().addActivityParams(drtActivityParams);

        // add taxi stage activity
        PlanCalcScoreConfigGroup.ActivityParams TaxiActivityParams = new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.taxi + " interaction");
        TaxiActivityParams.setTypicalDuration(1);
        TaxiActivityParams.setScoringThisActivityAtAll(false);
        config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(TaxiActivityParams));
        config.planCalcScore().addActivityParams(TaxiActivityParams);

        // add drt scoring parameters
        PlanCalcScoreConfigGroup.ModeParams drtModeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
        drtModeParams.setConstant(0.);
        drtModeParams.setMarginalUtilityOfDistance(0.);
        drtModeParams.setMarginalUtilityOfTraveling(0.);
        drtModeParams.setMonetaryDistanceRate(0.);
        config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtModeParams));

        // add taxi scoring parameter
        PlanCalcScoreConfigGroup.ModeParams taxiModeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.taxi);
        taxiModeParams.setConstant(0.);
        taxiModeParams.setMarginalUtilityOfDistance(0.);
        taxiModeParams.setMarginalUtilityOfTraveling(0.);
        taxiModeParams.setMonetaryDistanceRate(0.);
        config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(taxiModeParams));

        // set drt parameters
        DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
        drtCfg.getVehiclesFile();
        drtCfg.setVehiclesFile(drtVehiclesFile);
        drtCfg.setMaxTravelTimeAlpha(1.7);
        drtCfg.setMaxTravelTimeBeta(120.0);
        drtCfg.setStopDuration(60.);
        drtCfg.setMaxWaitTime(300.);
        drtCfg.setChangeStartLinkToLastLinkInSchedule(true);
        drtCfg.setIdleVehiclesReturnToDepots(false);
        drtCfg.setRequestRejection(false);
        drtCfg.setPrintDetailedWarnings(false);

        // set taxi parameters
        TaxiConfigGroup taxiCfg = TaxiConfigGroup.get(config);
        taxiCfg.setPickupDuration(120);
        taxiCfg.setDropoffDuration(60);
        taxiCfg.setTaxisFile(taxiVehicleFile);

        taxiCfg.addParameterSet(new AssignmentTaxiOptimizerParams());

        // set drt fare
        for (DrtFareConfigGroup drtFareCfg : DrtFaresConfigGroup.get(config).getDrtFareConfigGroups()) {
            drtFareCfg.setBasefare(0.);
            drtFareCfg.setDailySubscriptionFee(0.);
            drtFareCfg.setDistanceFare_m(0.0015);
            drtFareCfg.setMinFarePerTrip(4.0);
            drtFareCfg.setTimeFare_h(0.);
        }

        return config;
    }

    public static Scenario prepareScenario(Config config) {

        Scenario scenario = RunBerlinScenario.prepareScenario(config);

        // required by drt module
        scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());

        return scenario;
    }

    public static Controler prepareControler(Scenario scenario) {

        Controler controler = RunBerlinScenario.prepareControler(scenario);

        controler.addOverridingModule(new DvrpModule());
        controler.addOverridingModule(new DrtModule());
        controler.addOverridingModule(new DrtFareModule());
        controler.addOverridingModule(new TaxiModule());
        controler.addOverridingModule(new TaxiFareModule());



        String[] dvrpModes = {"taxi","drt"};
        controler.configureQSimComponents(DvrpQSimComponents.activateModes(dvrpModes));

        // use a main mode identifier which knows how to handle drt-specific legs as well as legs generated by the used sbb pt raptor router
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                bind(MainModeIdentifier.class).to(BerlinDrtMainModeIdentifier.class).asEagerSingleton();
            }
        });

        log.info("Done.");

        return controler;
    }
}

