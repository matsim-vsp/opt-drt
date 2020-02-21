package org.matsim.optDRT.util;

import com.google.inject.Inject;
import lombok.*;
import lombok.extern.java.Log;
import org.apache.commons.math3.analysis.function.Cos;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.optDRT.OptDrtConfigGroup;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor
@Log
public class ProfitUtility implements PersonMoneyEventHandler, LinkLeaveEventHandler, ActivityEndEventHandler, ActivityStartEventHandler , PersonDepartureEventHandler{
    @Inject
    OptDrtConfigGroup optDrtConfigGroup;
    @Inject
    Scenario scenario;

    private Map<Integer,Double> timeBin2Profit = new HashMap<>();
    private Map<Integer,Double> timeBin2Revenues = new HashMap<>();
    private Map<Integer,Double> timeBin2VariableCost = new HashMap<>();
    private Map<Integer,Double> timeBin2DrtStayTime = new HashMap<>();
    private Map<Integer,Double> timeBin2DrtMeters = new HashMap<>();

    @Getter
    private Map<Integer,Map<Integer,Double>> it2timeBin2Revenues = new HashMap<>();
    @Getter
    private Map<Integer,Map<Integer,Double>> it2timeBin2VariableCost = new HashMap<>();
    @Getter
    private Map<Integer,Map<Integer,Double>> it2timeBin2DrtStayTime = new HashMap<>();
    @Getter
    private Map<Integer,Map<Integer,Double>> it2timeBin2DrtMeters = new HashMap<>();
    @Getter
    private Map<Integer,Map<Integer,Map<CostType,Double>>> it2timeBin2CostType2Cost = new HashMap<>();

    private Map<Id<Person>,Set<DrtStayBehavior>> personId2DrtStayBehavior = new HashMap<>();
    private VariableCostCalculator variableCostCalculator;
    private Map<Id<Person>, Integer> drtUsers2DepartureTimeBin = new HashMap<>();


    private int currentIteration;

    @Override
    public void reset(int iteration) {
        this.currentIteration = iteration;
        this.variableCostCalculator = new VariableCostCalculator();

        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime());
        for(int tem = 0; tem <= timeBinSize; tem++){
            timeBin2VariableCost.put(tem,0.);
            timeBin2Profit.put(tem, 0.);
            timeBin2Revenues.put(tem, 0.);
            timeBin2DrtStayTime.put(tem, 0.);
            timeBin2DrtMeters.put(tem,0.);
        }

        this.it2timeBin2Revenues.put(iteration,timeBin2Revenues);
        this.it2timeBin2DrtStayTime.put(iteration,timeBin2DrtStayTime);
        this.it2timeBin2VariableCost.put(iteration,this.variableCostCalculator.getTimeBin2VariableCost());
        this.it2timeBin2CostType2Cost.put(iteration,this.variableCostCalculator.getTimeBin2Type2Cost());
        this.it2timeBin2DrtMeters.put(iteration,timeBin2DrtMeters);

    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if(linkLeaveEvent.getVehicleId().toString().contains("drt")){
            // this is a drt vehicle
            int timeBin = this.getTimeBin(linkLeaveEvent.getTime());
            this.variableCostCalculator.addCost(CostType.TRAVEL_DISTANCE, timeBin, this.timeBin2DrtMeters.get(timeBin) + scenario.getNetwork().getLinks().get(linkLeaveEvent.getLinkId()).getLength());
        }

    }

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {
        if(this.drtUsers2DepartureTimeBin.keySet().contains(personMoneyEvent.getPersonId())){
            int timeBin = this.drtUsers2DepartureTimeBin.get(personMoneyEvent.getPersonId());
            this.timeBin2Revenues.put(timeBin,this.timeBin2Revenues.get(timeBin)+ (-personMoneyEvent.getAmount()));
        }

    }

    @Override
    public void handleEvent(PersonDepartureEvent personDepartureEvent) {
        if(personDepartureEvent.getLegMode().toString().equals(optDrtConfigGroup.getOptDrtMode())){
            this.drtUsers2DepartureTimeBin.put(personDepartureEvent.getPersonId(),getTimeBin(personDepartureEvent.getTime()));
        }

    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {

        if(activityEndEvent.getActType().equals("DrtStay") && activityEndEvent.getPersonId().toString().contains("drt")){
            if(!this.personId2DrtStayBehavior.keySet().contains(activityEndEvent.getPersonId())){
                throw new RuntimeException("Error: DriverId should already be found");
            }

            var behaviors = this.personId2DrtStayBehavior.get(activityEndEvent.getPersonId());
            var thisBehavior = behaviors.stream().filter(b -> !b.isCompleteCreate()).collect(Collectors.toList());

            if(thisBehavior.size() != 1){
                throw new RuntimeException("DrtDriver : " + activityEndEvent.getPersonId() + " should has only one unfinished DrtStayBehavior now");
            } else {
                thisBehavior.get(0).setEndTime(activityEndEvent.getTime());
                thisBehavior.get(0).setCompleteCreate(true);

                double startTime = thisBehavior.get(0).getStartTime();
                double endTime = thisBehavior.get(0).getEndTime();
                int s = (int) (startTime/optDrtConfigGroup.getFareTimeBinSize());
                int e = (int) (endTime/optDrtConfigGroup.getFareTimeBinSize());

                if(s == e){
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s , (endTime - startTime));
                } else {
                    double stayTime1 = (s+1)*optDrtConfigGroup.getFareTimeBinSize() - startTime;
                    double stayTime2 = endTime - e * optDrtConfigGroup.getFareTimeBinSize();
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s , stayTime1);
                    this.variableCostCalculator.addCost(CostType.STAY_TIME, s , stayTime2);
                    for (int i = s+1; i <= e-1 ; i++) {
                        this.variableCostCalculator.addCost(CostType.STAY_TIME, i , optDrtConfigGroup.getFareTimeBinSize());
                    }
                }
            }
        }

    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {

        if(activityStartEvent.getActType().equals("DrtStay") && activityStartEvent.getPersonId().toString().contains("drt")){
            if(!this.personId2DrtStayBehavior.keySet().contains(activityStartEvent.getPersonId()))
                this.personId2DrtStayBehavior.put(activityStartEvent.getPersonId(),new HashSet<>());

            DrtStayBehavior drtStayBehavior = new DrtStayBehavior(activityStartEvent.getTime(),activityStartEvent.getPersonId());
            this.personId2DrtStayBehavior.get(activityStartEvent.getPersonId()).add(drtStayBehavior);

        }

    }

    private int getTimeBin(double time){
        return (int) ((int)time/optDrtConfigGroup.getFareTimeBinSize());
    }

    public enum CostType {
        TRAVEL_DISTANCE,STAY_TIME
    }

    public double getProfit(int iteration, int timeBin){
        return this.getIt2timeBin2Revenues().get(iteration).get(timeBin) + this.getIt2timeBin2VariableCost().get(iteration).get(timeBin);
    }

    public Map<Integer,Double> getProfit(int iteration){
        Map<Integer,Double> timeBin2Profit = new HashMap<>();
        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime());
        for(int tem = 0; tem <= timeBinSize; tem++){
            timeBin2Profit.put(tem, getProfit(iteration,tem));
        }
        return timeBin2Profit;
    }

    public Map<Integer,Map<Integer,Double>> getProfit(){
        Map<Integer,Map<Integer,Double>> it2timeBin2Profit = new HashMap<>();
        for (int a :
                this.it2timeBin2VariableCost.keySet()) {
            it2timeBin2Profit.put(a, getProfit(a));
        }
        return it2timeBin2Profit;
    }

    private Map<Integer,Map<Integer,Double>> getCostOfType(CostType costType){
        Map<Integer,Map<Integer,Double>> it2timeBin2Cost = new HashMap<>();
        for(int it : this.it2timeBin2CostType2Cost.keySet()){
            var newMap = new HashMap<Integer,Double>();
            this.it2timeBin2CostType2Cost.get(it).keySet().stream().forEach(timeBin -> {
                newMap.put(timeBin, this.it2timeBin2CostType2Cost.get(it).get(timeBin).get(costType));
            });
            it2timeBin2Cost.put(it,newMap);
        }
        return it2timeBin2Cost;
    }

    public void writeInfo(){
        String path = scenario.getConfig().controler().getOutputDirectory()+"profit/";
        if(!new File(path).exists()){
            new File(path).mkdirs();
        }
        String profitFile = path + "drtProfit.csv";
        String costFile = path +"drtCost.csv";
        String revenuesFile = path +"drtRevenues.csv";
        String stayTimeCost = path +"stayTimeCost.csv";
        String travelCost = path +"travelCost.csv";


        writeFile(profitFile,"profit",this.getProfit());
        writeFile(costFile,"cost",this.it2timeBin2VariableCost);
        writeFile(revenuesFile,"revenues", this.it2timeBin2Revenues);
        writeFile(stayTimeCost, "stayTime", this.getCostOfType(CostType.STAY_TIME));
        writeFile(travelCost, "travelCost", this.getCostOfType(CostType.TRAVEL_DISTANCE));


    }
    private void writeFile(String fileName, String type, Map<Integer,Map<Integer,Double>> map){

        try (FileWriter fileWriter = new FileWriter(fileName)) {
            BufferedWriter bw = new BufferedWriter(fileWriter);
            bw.write("it.,type,timeBin,num");
            map.forEach((it,map2) -> {
                map2.forEach((timeBin,value) -> {
                    try {
                        bw.newLine();
                        bw.write(it+","+type+","+timeBin+","+value);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            });
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @RequiredArgsConstructor
    @Setter
    @Getter
    private class DrtStayBehavior {
        @NonNull
        private double startTime;
        private double endTime;
        @NonNull
        private Id<Person> personId;
        private boolean completeCreate = false;
    }

    @Getter
    @Setter
    private class VariableCostCalculator{
        private Map<Integer,Map<CostType,Double>> timeBin2Type2Cost = new HashMap<>();
        private Map<Integer,Double> timeBin2VariableCost = new HashMap<>();

        public VariableCostCalculator() {
            int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime());
            for(int tem = 0; tem <= timeBinSize; tem++){
                Map<CostType,Double> type2cost = new HashMap<>();
                type2cost.put(CostType.STAY_TIME,0.);
                type2cost.put(CostType.TRAVEL_DISTANCE,0.);
                this.timeBin2Type2Cost.put(tem,type2cost);
                this.timeBin2VariableCost.put(tem,0.);
            }
        }
        private void addCost(CostType costType, int timeBin, double c){
            double cost = 0;
            if(costType.equals(CostType.STAY_TIME)){
                cost = -c * optDrtConfigGroup.getCostPerVehiclePerSecondFareAdjustment();
                double sumCost = this.timeBin2Type2Cost.get(timeBin).get(CostType.STAY_TIME) + cost;
                this.timeBin2Type2Cost.get(timeBin).put(CostType.STAY_TIME,sumCost);
            } else if (costType.equals(CostType.TRAVEL_DISTANCE)) {
                cost = -c * optDrtConfigGroup.getCostPerVehPerMeterForFleetAdjustment();
                double sumCost = this.timeBin2Type2Cost.get(timeBin).get(CostType.TRAVEL_DISTANCE) + cost;
                this.timeBin2Type2Cost.get(timeBin).put(CostType.TRAVEL_DISTANCE, sumCost);
            }
          this.timeBin2VariableCost.put(timeBin, this.timeBin2VariableCost.get(timeBin)+cost);
        }
    }
}
