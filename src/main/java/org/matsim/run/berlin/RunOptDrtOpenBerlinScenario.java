package org.matsim.run.berlin;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.optDRT.MultiModeOptDrtConfigGroup;
import org.matsim.optDRT.OptDrt;
import org.matsim.run.RunBerlinScenario;
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
		
		Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
		controler.run();
		RunBerlinScenario.runAnalysis(controler);
    }

    public Controler prepareControler(String[] args) {

		Config config = RunDrtOpenBerlinScenario.prepareConfig(args, new MultiModeOptDrtConfigGroup());

		Scenario scenario = RunDrtOpenBerlinScenario.prepareScenario(config);
		for (Person person : scenario.getPopulation().getPersons().values()) {
			person.getPlans().removeIf((plan) -> plan != person.getSelectedPlan());
		}

		Controler controler = RunDrtOpenBerlinScenario.prepareControler(scenario);

		OptDrt.addAsOverridingModule(controler, ConfigUtils.addOrGetModule(config, MultiModeOptDrtConfigGroup.class));

		return controler;
	}

}
