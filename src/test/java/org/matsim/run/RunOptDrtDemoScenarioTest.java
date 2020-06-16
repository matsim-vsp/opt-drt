package org.matsim.run;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.Controler;
import org.matsim.run.berlin.RunOptDrtOpenBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

/**
 *
 * @author zmeng
 *
 */
public class RunOptDrtDemoScenarioTest {
    private static final Logger log = Logger.getLogger(RunOptDrtDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void testModalSplitStrategyTo100pct() {
        // in this test, the fareAdjustmentModalSplitThreshold is 1.2

        String configFilename = "test/input/demo/optDrt-demo-fare-modalSplit-100pct.config.xml";
        final String[] args = {configFilename,
                "--config:global.coordinateSystem","Atlantis",
                "--config:network.inputNetworkFile","network_demo.xml",
                "--config.plan.inputPlansFile","plans-mixedDrtTaxi-demo.xml",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:strategy.strategysettings[strategyName=SubtourModeChoice].weight", "100",
                "--config:strategy.strategysettings[strategyName=ChangeExpBeta].strategyName", "BestScore",
                "--config:plans.inputPlansFile","plans-mixedDrtTaxi-demo.xml",
                "--config:subtourModeChoice.modes","drt,drt2",
                "--config:multiModeDrt.drt[mode=drt].vehiclesFile","drt-vehicles.xml",
                "--config:multiModeDrt.drt[mode=drt].operationalScheme","door2door",
                "--config:multiModeDrt.drt[mode=drt2].vehiclesFile","drt2-vehicles.xml",
                "--config:multiModeDrt.drt[mode=drt2].operationalScheme","door2door",
                "--config:controler.runId", "testModalSplitStrategyTo100pct",
                "--config:controler.lastIteration", "10",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
        controler.run();

        Map<String,String> it2DrtModeSplit = new HashMap<>();

        try {
            String modeStatsFile = utils.getOutputDirectory() + "testModalSplitStrategyTo100pct.modestats.txt";
            FileReader fileReader = new FileReader(modeStatsFile);
            BufferedReader br = new BufferedReader(fileReader);

            String line = br.readLine();
            List<String> array = Arrays.asList(line.split("\t"));
            int a = array.indexOf(TransportMode.drt);

            while ((line = br.readLine()) != null){
                List<String> modeArray = Arrays.asList(line.split("\t"));
                it2DrtModeSplit.put(modeArray.get(0), modeArray.get(a));
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Wrong modestats in final iteation.", true, it2DrtModeSplit.get("10").equals("1.0"));

        log.info("Done.");
        log.info("");

    }

    @Test
    public final void testModalSplitStrategyTo0pct() {
        // in this test, the fareAdjustmentModalSplitThreshold is -0.1

        String configFilename = "test/input/demo/optDrt-demo-fare-modalSplit-0pct.config.xml";
        final String[] args = {configFilename,
                "--config:global.coordinateSystem","Atlantis",
                "--config:network.inputNetworkFile","network_demo.xml",
                "--config.plan.inputPlansFile","plans-mixedDrtTaxi-demo.xml",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:strategy.strategysettings[strategyName=SubtourModeChoice].weight", "100",
                "--config:strategy.strategysettings[strategyName=ChangeExpBeta].strategyName", "BestScore",
                "--config:plans.inputPlansFile","plans-mixedDrtTaxi-demo.xml",
                "--config:subtourModeChoice.modes","drt,drt2",
                "--config:multiModeDrt.drt[mode=drt].vehiclesFile","drt-vehicles.xml",
                "--config:multiModeDrt.drt[mode=drt].operationalScheme","door2door",
                "--config:multiModeDrt.drt[mode=drt2].vehiclesFile","drt2-vehicles.xml",
                "--config:multiModeDrt.drt[mode=drt2].operationalScheme","door2door",
                "--config:controler.runId", "testModalSplitStrategyTo0pct",
                "--config:controler.lastIteration", "10",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
        controler.run();

        Map<String,String> it2DrtModeSplit = new HashMap<>();

        try {
            String modeStatsFile = utils.getOutputDirectory() + "testModalSplitStrategyTo0pct.modestats.txt";
            FileReader fileReader = new FileReader(modeStatsFile);
            BufferedReader br = new BufferedReader(fileReader);

            String line = br.readLine();
            List<String> array = Arrays.asList(line.split("\t"));
            int a = array.indexOf(TransportMode.drt);

            while ((line = br.readLine()) != null){
                List<String> modeArray = Arrays.asList(line.split("\t"));
                it2DrtModeSplit.put(modeArray.get(0), modeArray.get(a));
            }
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Assert.assertEquals("Wrong modestats in final iteation.", true, it2DrtModeSplit.get("10").equals("0.0"));

        log.info("Done.");
        log.info("");

    }

}
