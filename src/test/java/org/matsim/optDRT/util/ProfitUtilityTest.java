package org.matsim.optDRT.util;

import java.util.Random;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.run.RunOptDrtDemoScenarioTest;
import org.matsim.run.berlin.RunOptDrtOpenBerlinScenario;
import org.matsim.testcases.MatsimTestUtils;

public class ProfitUtilityTest {
    private static final Logger log = Logger.getLogger(RunOptDrtDemoScenarioTest.class);

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils();
    @Test
    public void testProfitUtil() {
        String configFilename = "test/input/demo/optDrt-planUtilTest.config.xml";
        final String[] args = {configFilename,
                "--config:global.coordinateSystem", "Atlantis",
                "--config:network.inputNetworkFile", "network_demo.xml",
                "--config:plans.inputPlansFile", "plans-allTaxi-demo.xml",
                "--config:controler.writeEventsInterval", "1",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:strategy.strategysettings[strategyName=SubtourModeChoice].weight", "100",
                "--config:strategy.strategysettings[strategyName=ChangeExpBeta].strategyName", "BestScore",
                "--config:subtourModeChoice.modes", "drt,drt2",
                "--config:multiModeDrt.drt[mode=drt].vehiclesFile", "20_drtVeh.xml",
                "--config:multiModeDrt.drt[mode=drt].operationalScheme", "door2door",
                "--config:multiModeDrt.drt[mode=drt2].vehiclesFile", "30_taxiVeh.xml",
                "--config:multiModeDrt.drt[mode=drt2].operationalScheme", "door2door",
                "--config:controler.runId", "testProfitUtil",
                "--config:controler.lastIteration", "5",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        ProfitUtility profitUtility = new ProfitUtility();

        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(profitUtility);
            }
        });
        controler.run();
        profitUtility.writeInfo();

    }

    @Test

    public void testProfitUtil_ModalSplit() {
        String configFilename = "test/input/demo/optDrt-planUtil_ModalSplitTest.config.xml";
        final String[] args = {configFilename,
                "--config:global.coordinateSystem", "Atlantis",
                "--config:network.inputNetworkFile", "network_demo.xml",
                "--config:plans.inputPlansFile", "demo-100-plans.xml",
                "--config:strategy.fractionOfIterationsToDisableInnovation", "0.8",
                "--config:strategy.strategysettings[strategyName=SubtourModeChoice].weight", "100",
                "--config:strategy.strategysettings[strategyName=ChangeExpBeta].strategyName", "BestScore",
                "--config:subtourModeChoice.modes", "drt,drt2",
                "--config:multiModeDrt.drt[mode=drt].vehiclesFile", "20_drtVeh.xml",
                "--config:multiModeDrt.drt[mode=drt].operationalScheme", "door2door",
                "--config:multiModeDrt.drt[mode=drt2].vehiclesFile", "30_taxiVeh.xml",
                "--config:multiModeDrt.drt[mode=drt2].operationalScheme", "door2door",
                "--config:controler.runId", "testProfitUtil",
                "--config:controler.lastIteration", "50",
                "--config:transit.useTransit", "false",
                "--config:controler.outputDirectory", utils.getOutputDirectory()};

        ProfitUtility profitUtility = new ProfitUtility();

        Controler controler = new RunOptDrtOpenBerlinScenario().prepareControler(args);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                this.addEventHandlerBinding().toInstance(profitUtility);
            }
        });
        controler.run();
        profitUtility.writeInfo();

    }





    public static void main(String[] args) {
        demoPlansFileWriter(100,"/Users/zhuoxiaomeng/IdeaProjects/opt-drt/test/input/demo/network_demo.xml");
    }
    private static void demoPlansFileWriter(int a, String networkFile){
        String plansFile = "demo-" + a + "-plans.xml";
        Network network = NetworkUtils.readNetwork(networkFile);
        Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
        Random random = new Random();

        for(int num = 0; num < a; num++){
            Link randomLink1 = network.getLinks().get(network.getLinks().keySet().toArray()[random.nextInt(network.getLinks().size())]);

            Link randomLink2 = network.getLinks().get(network.getLinks().keySet().toArray()[random.nextInt(network.getLinks().size())]);

            double endTime1 = random.nextInt(18*3600)%(18*3600-8*3600+1) + 8*3600;
            int endTime = (int)endTime1-1;
            double endTime2 = random.nextInt(endTime)%(endTime-8*3600+1) + 8*3600;

            Id<Person> personId = Id.create(num, Person.class);
            Person person = population.getFactory().createPerson(personId);
            person.getAttributes().putAttribute("subpopulation","person");

            Plan plan = population.getFactory().createPlan();
            Activity activity = population.getFactory().createActivityFromLinkId("home_600.0", randomLink1.getId());
            activity.setEndTime(endTime2);
            Leg leg = population.getFactory().createLeg("drt");
            Activity activity1 = population.getFactory().createActivityFromLinkId("work_600.0", randomLink2.getId());
            activity1.setEndTime(endTime1);
            Leg leg1 = population.getFactory().createLeg("drt");
            Activity activity2 = population.getFactory().createActivityFromLinkId("home_600.0", randomLink1.getId());
            plan.addActivity(activity);
            plan.addLeg(leg);
            plan.addActivity(activity1);
            plan.addLeg(leg1);
            plan.addActivity(activity2);
            person.addPlan(plan);
            population.addPerson(person);
        }
        new PopulationWriter(population).write(plansFile);

    }

}