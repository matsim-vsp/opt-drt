package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.Controler;
import org.matsim.run.example.RunOptDrtDemoScenario;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;

public class RunOptDrtDemoScenarioTest {
    private static final Logger log = Logger.getLogger(RunOptDrtDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void TestPositiveFare() {
        try {

            String configFilename = "test/input/demo/optDrt-demo-fare-dummy.config.xml";
            final String[] args = {configFilename,
                    "--config:plans.inputPlansFile","plans-allDrt-demo.xml",
                    "--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
                    "--config:controler.runId", "test0",
                    "--config:controler.lastIteration", "0",
                    "--config:transit.useTransit", "false",
                    "--config:controler.outputDirectory", utils.getOutputDirectory()};

            String drtVehiclesFile = "one_drt.xml";
            String taxiVehicleFile = "two_taxi.xml";

            Controler controler = new RunOptDrtDemoScenario().prepareControler(args, drtVehiclesFile, taxiVehicleFile);
            controler.run();

            // the score should be increased to almost 9999999.
            Assert.assertEquals("Wrong scores in final iteation.", true, 9000000 < controler.getScoreStats().getScoreHistory().get(ScoreItem.average).get(0));

            log.info("Done.");
            log.info("");

        } catch (Exception ee) {
            ee.printStackTrace();
            throw new RuntimeException(ee);
        }
    }
}
