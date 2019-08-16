package org.matsim.run.berlin;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.optDRT.OptDrtConfigGroup.ServiceAreaAdjustmentApproach;
import org.matsim.optDRT.OptDrtModule;
import org.matsim.optDRT.OptDrtServiceAreaStrategy;
import org.matsim.optDRT.OptDrtServiceAreaStrategyDemand;

/**
 *
 * @author ikaddoura
 */
public class RunOptDrtOpenBerlinScenario {
    private static final Logger log = Logger.getLogger(RunOptDrtOpenBerlinScenario.class);
    
    public static void main(String[] args) {
    	String[] arguments;
    	String drtVehiclesFile;
    	String drtServiceAreaShpFile;
    	
    	if ( args.length != 0 ){
    		
    		arguments = Arrays.copyOfRange( args, 0, args.length - 2 );
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
    
        new RunOptDrtOpenBerlinScenario().prepareControler(arguments, drtVehiclesFile, drtServiceAreaShpFile).run() ;
    }

    public Controler prepareControler(String[] args, String drtVehiclesFile, String drtServiceAreaShpFile) {
    	
    	Config config = RunDrtOpenBerlinScenario.prepareConfig(args, drtVehiclesFile);
    	config.addModule(new OptDrtConfigGroup());
    	
    	Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config, drtServiceAreaShpFile);
    	Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario, drtServiceAreaShpFile);
    	
        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
        controler.addOverridingModule(new OptDrtModule(optDrtConfigGroup));
              
		if (optDrtConfigGroup.getServiceAreaAdjustmentApproach() == ServiceAreaAdjustmentApproach.DemandThreshold) {			
			OptDrtServiceAreaStrategy optDrtServiceAreaStrategy = new OptDrtServiceAreaStrategyDemand(optDrtConfigGroup);
			
			controler.addOverridingQSimModule(new AbstractDvrpModeQSimModule(DrtConfigGroup.get(config).getMode()) {
				@Override
				protected void configureQSim() {
					this.bindModal(PassengerRequestValidator.class).toInstance((PassengerRequestValidator) optDrtServiceAreaStrategy);					
				}
			});
			
			controler.addOverridingModule(new AbstractModule() {		
				@Override
				public void install() {
					this.bind(OptDrtServiceAreaStrategy.class).toInstance(optDrtServiceAreaStrategy);
					this.addEventHandlerBinding().toInstance((EventHandler) optDrtServiceAreaStrategy);
				}
			});
			
		}
        
        return controler;
    }

}
