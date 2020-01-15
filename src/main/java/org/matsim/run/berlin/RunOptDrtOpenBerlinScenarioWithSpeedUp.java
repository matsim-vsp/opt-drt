package org.matsim.run.berlin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.drtSpeedUp.DrtSpeedUpConfigGroup;
import org.matsim.drtSpeedUp.DrtSpeedUpModule;
import org.matsim.optDRT.OptDrt;
import org.matsim.optDRT.OptDrtConfigGroup;
import org.matsim.run.RunBerlinScenario;
import org.matsim.run.drt.RunDrtOpenBerlinScenario;

/**
 *
 * @author ikaddoura
 */
public class RunOptDrtOpenBerlinScenarioWithSpeedUp {
    private static final Logger log = Logger.getLogger(RunOptDrtOpenBerlinScenarioWithSpeedUp.class);
    
    public static void main(String[] args) {
    	
    	for (String arg : args) {
			log.info( arg );
		}
		
		if ( args.length==0 ) {
			args = new String[] {"scenarios/berlin-v5.5-1pct/input/drt/berlin-drt-v5.5-1pct.config.xml"}  ;
		}
		
		Controler controler = new RunOptDrtOpenBerlinScenarioWithSpeedUp().prepareControler(args);
		controler.run();
		RunBerlinScenario.runAnalysis(controler);
    }

    public Controler prepareControler(String[] args) {
    	
    	Config config = RunDrtOpenBerlinScenario.prepareConfig(args, new OptDrtConfigGroup(), new DrtSpeedUpConfigGroup());
		DrtSpeedUpModule.adjustConfig(config);

    	Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);
    	for( Person person : scenario.getPopulation().getPersons().values() ){
			person.getPlans().removeIf( (plan) -> plan!=person.getSelectedPlan() ) ;
		}
    	
    	Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);
    	
		controler.addOverridingModule(new DrtSpeedUpModule());
    	
        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
    	OptDrt.addAsOverridingModule(controler, optDrtConfigGroup);
        
        return controler;
    }

}
