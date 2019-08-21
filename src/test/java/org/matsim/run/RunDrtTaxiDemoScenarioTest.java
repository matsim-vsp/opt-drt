package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.run.example.RunDrtTaxiDemoScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ZMeng
 *
 */
public class RunDrtTaxiDemoScenarioTest {
    private static final Logger log = Logger.getLogger(RunDrtTaxiDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void test0() {
        try {

            String configFilename = "test/input/demo/demo-taxi-drt.config.xml";
            final String[] args = {configFilename,
                    "--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
                    "--config:controler.runId", "test0",
                    "--config:controler.lastIteration", "20",
                    "--config:transit.useTransit", "false",
                    "--config:controler.outputDirectory", utils.getOutputDirectory()};

            String drtVehiclesFile = "one_drt.xml";
            String taxiVehicleFile = "two_taxi.xml";

            Config config = RunDrtTaxiDemoScenario.prepareConfig(args, drtVehiclesFile, taxiVehicleFile);
            Scenario scenario = RunDrtTaxiDemoScenario.prepareScenario(config);
            Controler controler = RunDrtTaxiDemoScenario.prepareControler(scenario);

            ModeAnalyzer modeAnalyzer = new ModeAnalyzer();

            controler.addOverridingModule(new AbstractModule() {

                @Override
                public void install() {
                    this.addEventHandlerBinding().toInstance(modeAnalyzer);
                }
            });

            controler.run();

            Assert.assertEquals("Wrong number of drt legs in final iteation.", 2, modeAnalyzer.getEnteredDrtVehicles());
            Assert.assertEquals("Wrong number of taxi legs in final iteation.", 2, modeAnalyzer.getEnteredTaxiVehicles());
            Assert.assertEquals("Wrong number of drt legs in 9. Iteration", 4, modeAnalyzer.getIt2enteredDrtPassengers().get(9).intValue());
            Assert.assertEquals("Wrong number of taxi legs in 19. Iteration", 4, modeAnalyzer.getIt2enteredTaxiPassengers().get(19).intValue());



            log.info("Done.");
            log.info("");

        } catch (Exception ee) {
            ee.printStackTrace();
            throw new RuntimeException(ee);
        }
    }
}
