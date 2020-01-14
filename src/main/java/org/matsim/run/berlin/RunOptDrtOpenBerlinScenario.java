package org.matsim.run.berlin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.optDRT.OptDrt;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

/**
 *
 * @author ikaddoura
 */
public class RunOptDrtOpenBerlinScenario {
    private static final Logger log = Logger.getLogger(RunOptDrtOpenBerlinScenario.class);
    
    public static void main(String[] args) {
    	
    	for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"}  ;
		}
		
        new RunOptDrtOpenBerlinScenario().prepareControler(args).run() ;
    }

    public Controler prepareControler(String[] args) {
    	
    	Config config = RunDrtOpenBerlinScenario.prepareConfig(args);
    	config.addModule(new OptDrtConfigGroup());
    	
    	Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);
    	Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);
    	
        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
    	OptDrt.addAsOverridingModule(controler, optDrtConfigGroup);
        
        return controler;
    }

}
