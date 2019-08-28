package org.matsim.run.example;

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
public class RunOptDrtDemoScenario {
    private static final Logger log = Logger.getLogger(RunOptDrtDemoScenario.class);

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
        new RunOptDrtDemoScenario().prepareControler(arguments, drtVehiclesFile, taxiVehicleFile).run() ;
    }

    public Controler prepareControler(String[] args, String drtVehiclesFile, String taxiVehicleFile) {

        Config config = RunDrtTaxiDemoScenario.prepareConfig(args, drtVehiclesFile,taxiVehicleFile);
        config.addModule(new OptDrtConfigGroup());

        Scenario scenario = RunDrtTaxiDemoScenario.prepareScenario(config);
        Controler controler = RunDrtTaxiDemoScenario.prepareControler(scenario);

        OptDrtConfigGroup optDrtConfigGroup = ConfigUtils.addOrGetModule(config, OptDrtConfigGroup.class);
        OptDrt.addAsOverridingModule(controler, optDrtConfigGroup);

        return controler;
    }

}
