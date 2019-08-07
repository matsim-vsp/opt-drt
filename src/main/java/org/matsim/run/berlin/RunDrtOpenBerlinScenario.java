/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.run.berlin;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareModule;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.DrtModule;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripRouter;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.optDRT.OptDrtModule;
import org.matsim.run.RunBerlinScenario;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * This class starts a simulation run with DRT.
 * 
 *  - The input DRT vehicles file specifies the number of vehicles and the vehicle capacity (a vehicle capacity of 1 means there is no ride-sharing).
 * 	- The DRT service area is set to the the inner-city Berlin area (see input shape file).
 * 	- Initial plans are not modified.
 * 
 * @author ikaddoura
 */

public final class RunDrtOpenBerlinScenario {

	private static final Logger log = Logger.getLogger(RunDrtOpenBerlinScenario.class);
	private final StageActivityTypes stageActivities = new StageActivityTypesImpl("pt interaction", "car interaction", "ride interaction");

	public static final String drtServiceAreaAttribute = "drtServiceArea";
	private final String drtNetworkMode = TransportMode.car;

	private final String drtServiceAreaShapeFile;
	
	private RunBerlinScenario berlin;

	public static void main(String[] args) {
		new RunDrtOpenBerlinScenario(args).run() ;
	}

	RunDrtOpenBerlinScenario( String [] args ) {
		
		String configFileName;
		
		if ( args.length != 0 ){
			configFileName = args[0];
			this.drtServiceAreaShapeFile = args[1];		
		} else {	
			configFileName = "path-to-config-file"; // start from matsim-berlin 5.3 version and extend by drt-required stuff
			this.drtServiceAreaShapeFile = "https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.3-10pct/input/berlin-shp/berlin.shp";			
		}
		
		this.berlin = new RunBerlinScenario(configFileName, null);			
	}

	 private void run() {
		
		// dvrp, drt, optDrt config groups
		ConfigGroup[] configGroups = {new DvrpConfigGroup(), new DrtConfigGroup(), new DrtFaresConfigGroup(), new OptDrtConfigGroup() };
		Config config = berlin.prepareConfig(configGroups);
			
		DrtConfigs.adjustDrtConfig(DrtConfigGroup.get(config), config.planCalcScore());
		
		Scenario scenario = berlin.prepareScenario();
		
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		
		BerlinShpUtils shpUtils = new BerlinShpUtils(drtServiceAreaShapeFile);
		new NetworkModification(shpUtils).addDRTmode(scenario, drtNetworkMode, drtServiceAreaAttribute);
		new PersonAttributesModification(shpUtils, stageActivities).run(scenario);
		
		Controler controler = berlin.prepareControler();
		
		// drt + dvrp module
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DvrpModule());
		controler.configureQSimComponents(
				DvrpQSimComponents.activateModes(DrtConfigGroup.get(controler.getConfig()).getMode()));

		// reject drt requests outside the service area
		controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(config).getMode()) {
			@Override
			protected void configureQSim() {
				bindModal(PassengerRequestValidator.class).toInstance(
						new ServiceAreaRequestValidator(drtServiceAreaAttribute));
			}
		});

		// Add drt-specific fare module
		controler.addOverridingModule(new DrtFareModule());
		
		// Add optDrt module
		OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
		controler.addOverridingModule(new OptDrtModule(optDrtConfigGroup));
		
		// different modes for different subpopulations
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				
				final Provider<TripRouter> tripRouterProvider = binder().getProvider(TripRouter.class);
				
				List<String> availableModesArrayList = new ArrayList<>();
				availableModesArrayList.add("bicycle");
				availableModesArrayList.add("pt");
				availableModesArrayList.add("walk");
				availableModesArrayList.add("car");
				
				final String[] availableModes = availableModesArrayList.toArray(new String[availableModesArrayList.size()]);
				
				addPlanStrategyBinding("SubtourModeChoice_no-potential-sav-user").toProvider(new Provider<PlanStrategy>() {
										
					@Inject
					Scenario sc;

					@Override
					public PlanStrategy get() {
						
						log.info("SubtourModeChoice_no-potential-sav-user" + " - available modes: " + availableModes.toString());
						final String[] chainBasedModes = {"car", "bicycle"};

						final Builder builder = new Builder(new RandomPlanSelector<>());
						builder.addStrategyModule(new SubtourModeChoice(sc.getConfig()
								.global()
								.getNumberOfThreads(), availableModes, chainBasedModes, false, 
								0.5, tripRouterProvider));
						builder.addStrategyModule(new ReRoute(sc, tripRouterProvider));
						return builder.build();
					}
				});			
			}
		});
		
		controler.run();
		
		log.info("Done.");
	}

}

