package org.matsim.run;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ZMeng
 *
 */
public class RunOptMultiDrtDemoScenarioTest {
    private static final Logger log = Logger.getLogger(RunOptMultiDrtDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();

    @Test
    public final void test0() {
        try {

            String configFilename = "test/input/demo/demo-drt2-drt.config.xml";
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
                    "--config:controler.runId", "test0",
                    "--config:controler.lastIteration", "10",
                    "--config:transit.useTransit", "false",
                    "--config:controler.outputDirectory", utils.getOutputDirectory()};

            Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);

            ModeAnalyzer modeAnalyzer = new ModeAnalyzer();

            controler.addOverridingModule(new AbstractModule() {

                @Override
                public void install() {
                    this.addEventHandlerBinding().toInstance(modeAnalyzer);
                }
            });

            controler.run();
            Assert.assertEquals("Wrong number of drt legs in final iteation.", 4, modeAnalyzer.getEnteredDrtVehicles());
            Assert.assertEquals("Wrong number of taxi legs in final iteation.", 0, modeAnalyzer.getEnteredTaxiVehicles());



            log.info("Done.");
            log.info("");

        } catch (Exception ee) {
            ee.printStackTrace();
            throw new RuntimeException(ee);
        }
    }
}
