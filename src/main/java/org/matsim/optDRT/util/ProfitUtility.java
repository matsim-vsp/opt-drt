package org.matsim.optDRT.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.optDRT.OptDrtConfigGroup;

import com.google.inject.Inject;

public class ProfitUtility implements PersonMoneyEventHandler, LinkLeaveEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, PersonDepartureEventHandler {

    private static final Logger log = Logger.getLogger(ProfitUtility.class);
    @Inject
    OptDrtConfigGroup optDrtConfigGroup;
    @Inject
    Scenario scenario;

    private Map<Integer, Map<Integer, Double>> it2timeBin2Revenues = new HashMap<>();
    private Map<Integer, Map<Integer, Double>> it2timeBin2VariableCost = new HashMap<>();
    private Map<Integer, Map<Integer, Map<CostType, Double>>> it2timeBin2CostType2Cost = new HashMap<>();
    private int currentIteration;

    private Map<Id<Person>, Set<DrtStayBehavior>> personId2DrtStayBehavior = new HashMap<>();
    private VariableCostCalculator variableCostCalculator;
    private Map<Id<Person>, Integer> drtUsers2DepartureTimeBin = new HashMap<>();
    private Map<Id<Person>, Set<PersonMoneyEvent>> personId2PersonMoneyEvents = new HashMap<>();


    @Override
    public void reset(int iteration) {
        this.variableCostCalculator = new VariableCostCalculator();
        drtUsers2DepartureTimeBin.clear();
        personId2PersonMoneyEvents.clear();
        this.currentIteration = iteration;

        this.it2timeBin2Revenues.put(iteration, new HashMap<>());
        this.it2timeBin2VariableCost.put(iteration, this.variableCostCalculator.getTimeBin2VariableCost());
        this.it2timeBin2CostType2Cost.put(iteration, this.variableCostCalculator.getTimeBin2Type2Cost());

        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime().seconds());
        for (int tem = 0; tem <= timeBinSize; tem++) {
            this.it2timeBin2Revenues.get(iteration).put(tem,0.);
        }




    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if (linkLeaveEvent.getVehicleId().toString().contains("drt")) {
            // this is a drt vehicle
            int timeBin = this.getTimeBin(linkLeaveEvent.getTime());
            this.variableCostCalculator.addCost(CostType.TRAVEL_DISTANCE, timeBin, scenario.getNetwork().getLinks().get(linkLeaveEvent.getLinkId()).getLength());
        }

    }

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {
        if (this.drtUsers2DepartureTimeBin.containsKey(personMoneyEvent.getPersonId())) {
            this.personId2PersonMoneyEvents.get(personMoneyEvent.getPersonId()).add(personMoneyEvent);
        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if (personDepartureEvent.getLegMode().equals(optDrtConfigGroup.getOptDrtMode())) {
            this.drtUsers2DepartureTimeBin.put(personDepartureEvent.getPersonId(), getTimeBin(personDepartureEvent.getTime()));
            this.personId2PersonMoneyEvents.put(personDepartureEvent.getPersonId(),new HashSet<>());
        }

    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {

        if (activityEndEvent.getActType().equals("DrtStay") && activityEndEvent.getPersonId().toString().contains("drt")) {
            if (!this.personId2DrtStayBehavior.containsKey(activityEndEvent.getPersonId())) {
                throw new RuntimeException("Error: DriverId should already be found");
            }

            var behaviors = this.personId2DrtStayBehavior.get(activityEndEvent.getPersonId());
            var thisBehavior = behaviors.stream().filter(b -> !b.isCompleteCreate()).collect(Collectors.toList());

            if (thisBehavior.size() != 1) {
                throw new RuntimeException("DrtDriver : " + activityEndEvent.getPersonId() + " should has only one unfinished DrtStayBehavior now");
            } else {
                thisBehavior.get(0).setEndTime(activityEndEvent.getTime());
                thisBehavior.get(0).setCompleteCreate(true);

                double startTime = thisBehavior.get(0).getStartTime();
                double endTime = thisBehavior.get(0).getEndTime();
                int s = (int) (startTime / optDrtConfigGroup.getFareTimeBinSize());
                int e = (int) (endTime / optDrtConfigGroup.getFareTimeBinSize());

                if (s == e) {
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s, (endTime - startTime));
                } else {
                    double stayTime1 = (s + 1) * optDrtConfigGroup.getFareTimeBinSize() - startTime;
                    double stayTime2 = endTime - e * optDrtConfigGroup.getFareTimeBinSize();
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s, stayTime1);
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s, stayTime2);
                    for (int i = s + 1; i <= e - 1; i++) {
                        this.variableCostCalculator.addCost(CostType.STAY_TIME, i, optDrtConfigGroup.getFareTimeBinSize());
                    }
                }
            }
        }

    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

        if (activityStartEvent.getActType().equals("DrtStay") && activityStartEvent.getPersonId().toString().contains("drt")) {
            if (!this.personId2DrtStayBehavior.containsKey(activityStartEvent.getPersonId()))
                this.personId2DrtStayBehavior.put(activityStartEvent.getPersonId(), new HashSet<>());

            DrtStayBehavior drtStayBehavior = new DrtStayBehavior(activityStartEvent.getTime(), activityStartEvent.getPersonId());
            this.personId2DrtStayBehavior.get(activityStartEvent.getPersonId()).add(drtStayBehavior);

        } else if ((!activityStartEvent.getActType().contains("drt interaction")) && this.personId2PersonMoneyEvents.containsKey(activityStartEvent.getPersonId())) {
            int timeBin = this.drtUsers2DepartureTimeBin.get(activityStartEvent.getPersonId());
            double currentRevenues = this.it2timeBin2Revenues.get(currentIteration).get(timeBin);
            double temAmount = this.personId2PersonMoneyEvents.get(activityStartEvent.getPersonId()).stream().mapToDouble(personMoneyEvent -> - personMoneyEvent.getAmount()).sum();
            this.it2timeBin2Revenues.get(currentIteration).put(timeBin, currentRevenues + temAmount);
            this.drtUsers2DepartureTimeBin.remove(activityStartEvent.getPersonId());
            this.personId2PersonMoneyEvents.remove(activityStartEvent.getPersonId());
        }

    }

    private int getTimeBin(double time) {
        return (int) ((int) time / optDrtConfigGroup.getFareTimeBinSize());
    }

    public enum CostType {
        TRAVEL_DISTANCE, STAY_TIME
    }

    public double getProfit(int iteration, int timeBin) {
        return this.getIt2timeBin2Revenues().get(iteration).get(timeBin) + this.getIt2timeBin2VariableCost().get(iteration).get(timeBin);
    }

    public Map<Integer, Double> getProfit(int iteration) {
        Map<Integer, Double> timeBin2Profit = new HashMap<>();
        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime().seconds());
        for (int tem = 0; tem <= timeBinSize; tem++) {
            timeBin2Profit.put(tem, getProfit(iteration, tem));
        }
        return timeBin2Profit;
    }

    public Map<Integer, Map<Integer, Double>> getProfit() {
        Map<Integer, Map<Integer, Double>> it2timeBin2Profit = new HashMap<>();
        for (int a :
                this.it2timeBin2VariableCost.keySet()) {
            it2timeBin2Profit.put(a, getProfit(a));
        }
        return it2timeBin2Profit;
    }

    private Map<Integer, Map<Integer, Double>> getCostOfType(CostType costType) {
        Map<Integer, Map<Integer, Double>> it2timeBin2Cost = new HashMap<>();
        for (int it : this.it2timeBin2CostType2Cost.keySet()) {
            var newMap = new HashMap<Integer, Double>();
            this.it2timeBin2CostType2Cost.get(it).keySet().forEach(timeBin -> newMap.put(timeBin, this.it2timeBin2CostType2Cost.get(it).get(timeBin).get(costType)));
            it2timeBin2Cost.put(it, newMap);
        }
        return it2timeBin2Cost;
    }

    public void writeInfo() {
        String path = scenario.getConfig().controler().getOutputDirectory() + "profit/";
        if (!new File(path).exists()) {
            new File(path).mkdirs();
        }
        String profitFile = path + "drtProfit.csv";
        String costFile = path + "drtCost.csv";
        String revenuesFile = path + "drtRevenues.csv";
        String stayTimeCost = path + "stayTimeCost.csv";
        String travelCost = path + "travelCost.csv";


        writeFile(profitFile, "profit", this.getProfit());
        writeFile(costFile, "cost", this.it2timeBin2VariableCost);
        writeFile(revenuesFile, "revenues", this.it2timeBin2Revenues);
        writeFile(stayTimeCost, "stayTime", this.getCostOfType(CostType.STAY_TIME));
        writeFile(travelCost, "travelCost", this.getCostOfType(CostType.TRAVEL_DISTANCE));


    }

    private void writeFile(String fileName, String type, Map<Integer, Map<Integer, Double>> map) {

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.write("it.,type,timeBin,num");
            map.forEach((it, map2) -> map2.forEach((timeBin, value) -> {
                try {
                    bw.newLine();
                    bw.write(it + "," + type + "," + timeBin + "," + value);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }));
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, Map<Integer, Double>> getIt2timeBin2Revenues() {
        return it2timeBin2Revenues;
    }

    public Map<Integer, Map<Integer, Double>> getIt2timeBin2VariableCost() {
        return it2timeBin2VariableCost;
    }

    private static class DrtStayBehavior {
        private double startTime;
        private double endTime;
        private Id<Person> personId;
        private boolean completeCreate = false;

        public DrtStayBehavior(double startTime, Id<Person> personId) {
            this.startTime = startTime;
            this.personId = personId;
        }

        public double getStartTime() { return startTime; }
        public void setStartTime(double startTime) { this.startTime = startTime; }
        public double getEndTime() { return endTime; }
        public void setEndTime(double endTime) { this.endTime = endTime; }
        public Id<Person> getPersonId() { return personId; }
        public void setPersonId(Id<Person> personId) { this.personId = personId;}
        public boolean isCompleteCreate() { return completeCreate; }
        public void setCompleteCreate(boolean completeCreate) { this.completeCreate = completeCreate;}
    }


    private class VariableCostCalculator {
        private Map<Integer, Map<CostType, Double>> timeBin2Type2Cost = new HashMap<>();
        private Map<Integer, Double> timeBin2VariableCost = new HashMap<>();

        public VariableCostCalculator() {
            int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime().seconds());
            for (int tem = 0; tem <= timeBinSize; tem++) {
                Map<CostType, Double> type2cost = new HashMap<>();
                type2cost.put(CostType.STAY_TIME, 0.);
                type2cost.put(CostType.TRAVEL_DISTANCE, 0.);
                this.timeBin2Type2Cost.put(tem, type2cost);
                this.timeBin2VariableCost.put(tem, 0.);
            }
        }

        private void addCost(CostType costType, int timeBin, double c) {
            double cost = 0;
            if (costType.equals(CostType.STAY_TIME)) {
                cost = -c * optDrtConfigGroup.getCostPerVehiclePerSecondFareAdjustment();
                double sumCost = this.timeBin2Type2Cost.get(timeBin).get(CostType.STAY_TIME) + cost;
                this.timeBin2Type2Cost.get(timeBin).put(CostType.STAY_TIME, sumCost);
            } else if (costType.equals(CostType.TRAVEL_DISTANCE)) {
                cost = -c * optDrtConfigGroup.getCostPerVehPerMeterForFleetAdjustment();
                double sumCost = this.timeBin2Type2Cost.get(timeBin).get(CostType.TRAVEL_DISTANCE) + cost;
                this.timeBin2Type2Cost.get(timeBin).put(CostType.TRAVEL_DISTANCE, sumCost);
            }
            this.timeBin2VariableCost.put(timeBin, this.timeBin2VariableCost.get(timeBin) + cost);
        }

        public Map<Integer, Map<CostType, Double>> getTimeBin2Type2Cost() {
            return timeBin2Type2Cost;
        }

        public Map<Integer, Double> getTimeBin2VariableCost() {
            return timeBin2VariableCost;
        }

    }
}
