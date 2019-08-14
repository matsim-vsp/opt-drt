package org.matsim.run.berlin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.optDRT.OptDrtModule;
import org.matsim.run.RunBerlinScenario;

/**
 *
 * @author ikaddoura
 */
public class RunExampleOptDrtOpenBerlinScenario {
    private static final Logger log = Logger.getLogger(RunExampleOptDrtOpenBerlinScenario.class);
    
    public static void main(String[] args) {
    	String[] arguments;
    	String drtVehiclesFile;
    	String drtServiceAreaShpFile;
    	
    	if ( args.length != 0 ){
    		
    		arguments = Arrays.copyOfRange( args, 0, args.length - 3 );
    		log.info("arguments: " + arguments.toString());
    		
    		drtVehiclesFile = args[args.length - 2];
    		log.info("drtVehiclesFile: " + drtVehiclesFile);
    		
    		drtServiceAreaShpFile = args[args.length - 1];      
    		log.info("drtAreaShpFile: " + drtServiceAreaShpFile);

    	} else {
    		arguments = new String[] {"scenarios/berlin-v5.4-1pct/input/berlin-v5.4-1pct.config.xml"};
            drtVehiclesFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/berlkoenig-vehicles/berlin-v5.2.berlkoenig100veh_6seats.xml.gz";
        	drtServiceAreaShpFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/projects/avoev/berlin-sav-v5.2-10pct/input/shp-berlkoenig-area/berlkoenig-area.shp";
        }    	
    
        new RunExampleOptDrtOpenBerlinScenario().run(arguments, drtVehiclesFile, drtServiceAreaShpFile) ;
    }

    public Controler run(String[] args, String drtVehiclesFile, String drtServiceAreaShpFile) {
    	
    	Config config = RunBerlinScenario.prepareConfig(args);
    	config.addModule(new DvrpConfigGroup());
    	config.addModule(new DrtConfigGroup());
    	config.addModule(new DrtFaresConfigGroup());
//    	config.addModule(new OptDrtConfigGroup());
    	
    	// add drt mode	
    	List<String> modes = new ArrayList<String>(Arrays.asList(config.subtourModeChoice().getModes()));
    	modes.add(TransportMode.drt);
    	config.subtourModeChoice().setModes(modes.toArray(new String[modes.size()]));
    	
    	// required by drt module
    	config.qsim().setNumberOfThreads(1);
    	config.qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);
    	DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
    	
    	// add drt stage activity (per default only added in case of stop-based drt operation mode)
    	PlanCalcScoreConfigGroup.ActivityParams params = new PlanCalcScoreConfigGroup.ActivityParams(TransportMode.drt + " interaction");
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addActivityParams(params));
		config.planCalcScore().addActivityParams(params);
		
		// add drt scoring parameters
		PlanCalcScoreConfigGroup.ModeParams drtModeParams = new PlanCalcScoreConfigGroup.ModeParams(TransportMode.drt);
		drtModeParams.setConstant(0.);
		drtModeParams.setMarginalUtilityOfDistance(0.);
		drtModeParams.setMarginalUtilityOfTraveling(0.);
		drtModeParams.setMonetaryDistanceRate(0.);
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtModeParams));
    	    	
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
    	    	
    	// set drt fare
    	for (DrtFareConfigGroup drtFareCfg : DrtFaresConfigGroup.get(config).getDrtFareConfigGroups()) {
    		drtFareCfg.setBasefare(0.);
        	drtFareCfg.setDailySubscriptionFee(0.);
        	drtFareCfg.setDistanceFare_m(0.0015);
        	drtFareCfg.setMinFarePerTrip(4.0);
        	drtFareCfg.setTimeFare_h(0.);
    	}
    	
    	Scenario scenario = RunBerlinScenario.prepareScenario(config);
    	
    	// required by drt module
    	scenario.getPopulation().getFactory().getRouteFactories().setRouteFactory(DrtRoute.class, new DrtRouteFactory());
           
		String drtServiceAreaAttribute = "drtServiceArea";
        addDRTServiceAreaParameterToCarLinks(scenario, drtServiceAreaAttribute, drtServiceAreaShpFile);
    	
    	Controler controler = RunBerlinScenario.prepareControler(scenario);
    	
    	// add drt module (and related modules)
    	controler.addOverridingModule(new DvrpModule());
		controler.addOverridingModule(new DrtModule());
		controler.configureQSimComponents(DvrpQSimComponents.activateModes(drtCfg.getMode()));
		
		// add drt fare module
        controler.addOverridingModule(new DrtFareModule());
        
        // only serve drt requests within the service area
        controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(config).getMode()) {
 			@Override
 			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class).toInstance(
 						new ServiceAreaRequestValidator(drtServiceAreaAttribute));
 			}
 		});

        // optDrt module
//        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
//        controler.addOverridingModule(new OptDrtModule(optDrtConfigGroup));

        controler.run();

        log.info("Done.");
        
        return controler;
    }
    	
	private void addDRTServiceAreaParameterToCarLinks(Scenario scenario, String serviceAreaAttribute, String drtServiceAreaShpFile) {
		
		log.info("Loading drt service area shape file...");
		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShpFile);    	
    	log.info("Loading drt service area shape file... Done.");
		
		log.info("Adding drt service area parameter to links...");

		int counter = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (counter % 10000 == 0)
				log.info("link #" + counter);
			counter++;
			if (link.getAllowedModes().contains(TransportMode.car)) {
	
				if (shpUtils.isCoordInDrtServiceArea(link.getFromNode().getCoord())
						|| shpUtils.isCoordInDrtServiceArea(link.getToNode().getCoord())) {
					link.getAttributes().putAttribute(serviceAreaAttribute, true);
				} else {
					link.getAttributes().putAttribute(serviceAreaAttribute, false);
				}
			}
		}
		
		log.info("Adding drt service area parameter to links... Done.");
	}

}
