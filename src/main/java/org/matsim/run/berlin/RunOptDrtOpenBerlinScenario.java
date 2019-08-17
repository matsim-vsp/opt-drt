package org.matsim.run.berlin;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.optDRT.OptDrt;
import org.matsim.optDRT.OptDrtConfigGroup;

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
    	OptDrt.addAsOverridingModule(controler, optDrtConfigGroup);
        
        return controler;
    }

}
