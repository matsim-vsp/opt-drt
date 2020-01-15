package org.matsim.run;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.analysis.ScoreStatsControlerListener.ScoreItem;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.run.example.RunOptDrtDemoScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * @author ikaddoura
 *
 */
public class RunOptDrtDemoScenarioTest {
    private static final Logger log = Logger.getLogger(RunOptDrtDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Ignore // TODO: Fixme!
    @Test
    public final void testPositiveFare() {
        try {

            String configFilename = "test/input/demo/optDrt-demo-fare-dummy.config.xml";
            final String[] args = {configFilename,
                    "--config:plans.inputPlansFile","plans-allDrt-demo.xml",
                    "--config:strategy.fractionOfIterationsToDisableInnovation", "1.0",
                    "--config:controler.runId", "testPositiveFare",
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
    @Ignore // TODO: Fixme!
    @Test
    public final void testModalSplitStrategyTo100pct() {
        // in this test, the fareAdjustmentModalSplitThreshold is 1.2
        String configFilename = "test/input/demo/optDrt-demo-fare-modalSplit-100pct.config.xml";
        final String[] args = {configFilename,
                "--config:plans.inputPlansFile", "plans-allDrt-demo.xml",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:controler.runId", "testModalSplitStrategyTo100pct",
                "--config:controler.lastIteration", "10",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        String drtVehiclesFile = "one_drt.xml";
        String taxiVehicleFile = "two_taxi.xml";

        Controler controler = new RunOptDrtDemoScenario().prepareControler(args, drtVehiclesFile, taxiVehicleFile);
        controler.run();

        Map<String,String> it2DrtModeSplit = new HashMap<>();

        try {
            String modeStatsFile = "test/output/org/matsim/run/RunOptDrtDemoScenarioTest/testModalSplitStrategyTo100pct/testModalSplitStrategyTo100pct.modestats.txt";
            File filename = new File(modeStatsFile);
            FileReader fileReader = new FileReader(modeStatsFile);
            BufferedReader br = new BufferedReader(fileReader);

            String line = br.readLine();
            List<String> array = Arrays.asList(line.split("\t"));
            int a = array.indexOf(TransportMode.drt);

            while ((line = br.readLine()) != null){
                List<String> modeArray = Arrays.asList(line.split("\t"));
                it2DrtModeSplit.put(modeArray.get(0), modeArray.get(a));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Wrong modestats in final iteation.", true, it2DrtModeSplit.get("10").equals("1.0"));

        log.info("Done.");
        log.info("");

    }
    @Ignore // TODO: Fixme!
    @Test
    public final void testModalSplitStrategyTo0pct() {
        // in this test, the fareAdjustmentModalSplitThreshold is 0.0
        String configFilename = "test/input/demo/optDrt-demo-fare-modalSplit-0pct.config.xml";
        final String[] args = {configFilename,
                "--config:plans.inputPlansFile", "plans-allDrt-demo.xml",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:controler.runId", "testModalSplitStrategyTo0pct",
                "--config:controler.lastIteration", "20",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        String drtVehiclesFile = "one_drt.xml";
        String taxiVehicleFile = "two_taxi.xml";

        Controler controler = new RunOptDrtDemoScenario().prepareControler(args, drtVehiclesFile, taxiVehicleFile);
        controler.run();

        Map<String,String> it2DrtModeSplit = new HashMap<>();

        try {
            String modeStatsFile = "test/output/org/matsim/run/RunOptDrtDemoScenarioTest/testModalSplitStrategyTo0pct/testModalSplitStrategyTo0pct.modestats.txt";
            File filename = new File(modeStatsFile);
            FileReader fileReader = new FileReader(modeStatsFile);
            BufferedReader br = new BufferedReader(fileReader);

            String line = br.readLine();
            List<String> array = Arrays.asList(line.split("\t"));
            int a = array.indexOf(TransportMode.drt);

            while ((line = br.readLine()) != null){
                List<String> modeArray = Arrays.asList(line.split("\t"));
                it2DrtModeSplit.put(modeArray.get(0), modeArray.get(a));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Wrong modestats in final iteation.", "0.0", it2DrtModeSplit.get("10"));

        log.info("Done.");
        log.info("");

    }
}
