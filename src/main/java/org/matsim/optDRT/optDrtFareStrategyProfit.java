package org.matsim.optDRT;

import com.google.inject.Inject;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFareConfigGroup;
import org.matsim.contrib.av.robotaxi.fares.drt.DrtFaresConfigGroup;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.optDRT.util.ProfitUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class optDrtFareStrategyProfit implements PersonArrivalEventHandler, OptDrtFareStrategy, PersonDepartureEventHandler, DrtRequestSubmittedEventHandler {

    private final Logger logger = Logger.getLogger(optDrtFareStrategyProfit.class);
    @Inject
    ProfitUtility profitUtility;
    @Inject
    OptDrtConfigGroup optDrtConfigGroup;
    @Inject
    Config config;
    @Inject
    DrtFaresConfigGroup drtFaresConfigGroup;
    @Inject
    EventsManager events;

    private int currentIteration;
    private boolean updateFare;
    private Map<Integer,Map<Integer,Double>> it2TimeBin2Fare = new HashMap<>();
    private Map<Integer,Double> timeBin2distanceFarePerMeter = new HashMap<>();
    private Map<Id<Person>, DrtRequestSubmittedEvent> lastRequestSubmission = new HashMap<>();
    private Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
    private Map<Integer, Double> timeBin2drtTrips = new HashMap<>();


    @Override
    public void reset(int iteration) {
        iteration = this.currentIteration;
        this.it2TimeBin2Fare.put(iteration,this.timeBin2distanceFarePerMeter);
    }

    @Override
    public void handleEvent(PersonArrivalEvent personArrivalEvent) {

        if (personArrivalEvent.getLegMode().equals(optDrtConfigGroup.getOptDrtMode())) {

            DrtRequestSubmittedEvent e = this.lastRequestSubmission.get(personArrivalEvent.getPersonId());

            int timeBin = getTimeBin(drtUserDepartureTime.get(personArrivalEvent.getPersonId()));
            this.timeBin2drtTrips.put(timeBin, this.timeBin2drtTrips.get(timeBin) + 1);

            double timeBinDistanceFare = 0;
            if (this.timeBin2distanceFarePerMeter.get(timeBin) != null) {
                timeBinDistanceFare = this.timeBin2distanceFarePerMeter.get(timeBin);
            }
            // update the price, and make sure the new price will not be lower than the minFare in drtFareConfig.
            double fare = e.getUnsharedRideDistance() * timeBinDistanceFare;
            DrtFareConfigGroup drtFareConfigGroup = drtFaresConfigGroup.getDrtFareConfigGroups().stream().filter(drtFareConfigGroup1 -> drtFareConfigGroup1.getMode().equals("drt")).collect(Collectors.toList()).get(0);

            double oldFare = Math.max(e.getUnsharedRideDistance() * drtFareConfigGroup.getDistanceFare_m(), drtFareConfigGroup.getMinFarePerTrip());
            if (oldFare + fare < drtFareConfigGroup.getMinFarePerTrip()) {
                events.processEvent(new PersonMoneyEvent(personArrivalEvent.getTime(), personArrivalEvent.getPersonId(), ((-drtFareConfigGroup.getMinFarePerTrip())) + oldFare));
            } else {
                events.processEvent(new PersonMoneyEvent(personArrivalEvent.getTime(), personArrivalEvent.getPersonId(), -fare));
            }

            this.drtUserDepartureTime.remove(personArrivalEvent.getPersonId());
            this.lastRequestSubmission.remove(personArrivalEvent.getPersonId());
        }
    }

    private int getTimeBin(Double time) {
        int timeBin = (int) (time / optDrtConfigGroup.getFareTimeBinSize());
        return timeBin;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (optDrtConfigGroup.getOptDrtMode().equals(event.getLegMode())) {
            this.drtUserDepartureTime.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {
        if (optDrtConfigGroup.getOptDrtMode().equals(drtRequestSubmittedEvent.getMode())) {
            this.lastRequestSubmission.put(drtRequestSubmittedEvent.getPersonId(), drtRequestSubmittedEvent);
        }
    }

    @Override
    public void updateFares() {
        this.updateFare = true;


    }

    @Override
    public void writeInfo() {

    }

    @RequiredArgsConstructor
    class drtProfitStatsInTimeBin{
        @NonNull
        int timeBin;
        Map<Integer,Double> it2Profit = new HashMap<>();
        Map<Integer,Double> it2Revenues = new HashMap<>();
        Map<Integer,Double> it2VariableCost = new HashMap<>();

        private void updateStats(int I){

        }

    }
}
