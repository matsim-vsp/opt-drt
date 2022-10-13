package org.matsim.optDRT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.fare.DrtFareParams;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

/**
 * @author zmeng
 * <p>
 * An implementation for different DRT fares for different times of day.
 * The fares will be updated during the simulation depending on modestats.
 * <p>
 * Note that these fares are scored in excess to anything set in the modeparams in the config file or any other drt fare handler.
 */
public class OptDrtFareStrategyModalSplit implements PersonDepartureEventHandler, PersonArrivalEventHandler, OptDrtFareStrategy, DrtRequestSubmittedEventHandler {
    private static final Logger log = LogManager.getLogger(OptDrtFareStrategyModalSplit.class);

    private Map<Integer, Double> timeBin2distanceFarePerMeter = new HashMap<>();

    private Map<Id<Person>, DrtRequestSubmittedEvent> lastRequestSubmission = new HashMap<>();
    private Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
    private Map<Integer, Double> timeBin2DrtModalStats = new HashMap<>();
    private Map<Integer, Double> timeBin2totalTrips = new HashMap<>();
    private Map<Integer, Double> timeBin2drtTrips = new HashMap<>();
    private Map<Map<Id<Person>, String>, Double> personDepartureInfo = new HashMap<>();

    private final List<String> mainTransportModes = new LinkedList<>();

    private Set<Id<Person>> personList;

    private final OptDrtConfigGroup optDrtConfigGroup;

    private final EventsManager events;

    private final Scenario scenario;

    private final MainModeIdentifier mainModeIdentifier;

    private final DrtFareParams drtFareConfigGroup;

    public OptDrtFareStrategyModalSplit(OptDrtConfigGroup optDrtConfigGroup, EventsManager events, Scenario scenario,
            MainModeIdentifier mainModeIdentifier, DrtFareParams drtFareConfigGroup) {
        this.optDrtConfigGroup = optDrtConfigGroup;
        this.events = events;
        this.scenario = scenario;
        this.mainModeIdentifier = mainModeIdentifier;
        this.drtFareConfigGroup = drtFareConfigGroup;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {

        Map<Id<Person>, String> personId2Legmode = new HashMap<>();
        personId2Legmode.put(event.getPersonId(), event.getLegMode());

        if (this.personDepartureInfo.containsKey(personId2Legmode)) {
            int timeBin = getTimeBin(personDepartureInfo.get(personId2Legmode));
            this.timeBin2totalTrips.put(timeBin, timeBin2totalTrips.get(timeBin) + 1);
            this.personDepartureInfo.remove(personId2Legmode);
        }

        if (event.getLegMode().equals(optDrtConfigGroup.getMode())) {

            DrtRequestSubmittedEvent e = this.lastRequestSubmission.get(event.getPersonId());

            int timeBin = getTimeBin(drtUserDepartureTime.get(event.getPersonId()));
            this.timeBin2drtTrips.put(timeBin, this.timeBin2drtTrips.get(timeBin) + 1);

            double timeBinDistanceFare = 0;
            if (this.timeBin2distanceFarePerMeter.get(timeBin) != null) {
                timeBinDistanceFare = this.timeBin2distanceFarePerMeter.get(timeBin);
            }
            // update the price, and make sure the new price will not be lower than the minFare in drtFareConfig.
            double fare = e.getUnsharedRideDistance() * timeBinDistanceFare;

            double oldFare = Math.max(e.getUnsharedRideDistance() * drtFareConfigGroup.distanceFare_m, drtFareConfigGroup.minFarePerTrip);
            if (oldFare + fare < drtFareConfigGroup.minFarePerTrip) {
                events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), ((-drtFareConfigGroup.minFarePerTrip)) + oldFare, "opt-drt-fare-surcharge", this.optDrtConfigGroup.getMode() + "-operator"));
            } else {
                events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare, "opt-drt-fare-surcharge", this.optDrtConfigGroup.getMode() + "-operator"));
            }

            this.drtUserDepartureTime.remove(event.getPersonId());
            this.lastRequestSubmission.remove(event.getPersonId());
        }
    }

    private int getTimeBin(Double time) {
        int timeBin = (int) (time / optDrtConfigGroup.getFareTimeBinSize());
        return timeBin;
    }

    @Override
    public void reset(int iteration) {}

    @Override
    public void resetDataForThisIteration( int currentIteration ) {
        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime().seconds());
        for (int timeBin = 0; timeBin <= timeBinSize; timeBin++) {
            this.timeBin2DrtModalStats.put(timeBin, 0.);
            this.timeBin2totalTrips.put(timeBin, 0.);
            this.timeBin2drtTrips.put(timeBin, 0.);
        }
        lastRequestSubmission.clear();
        drtUserDepartureTime.clear();

        // collect real person Id
        this.personList = this.scenario.getPopulation().getPersons().keySet();
        log.info("-- active persons in " + currentIteration + ".it are " + Arrays.toString(personList.toArray()));

        // record main modes in this iteration
        List<TripStructureUtils.Trip> trips = new LinkedList<>();
        for (Person person :
                this.scenario.getPopulation().getPersons().values()) {
            trips.addAll(TripStructureUtils.getTrips((person.getSelectedPlan())));
        }
        for (TripStructureUtils.Trip t :
                trips) {
            String mode = this.mainModeIdentifier.identifyMainMode(t.getTripElements());
            if (!this.mainTransportModes.contains(mode))
                this.mainTransportModes.add(mode);
        }
        log.info("-- main mode in " + currentIteration + ".it are " + Arrays.toString(this.mainTransportModes.toArray()));
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if (optDrtConfigGroup.getMode().equals(event.getMode())) {
            this.lastRequestSubmission.put(event.getPersonId(), event);
        }
    }

    @Override
    public void updateFares( int currentIteration ) {
        for (int timeBin = 0; timeBin <= getTimeBin(scenario.getConfig().qsim().getEndTime().seconds()); timeBin++) {
            double drtModeStats = timeBin2DrtModalStats.get(timeBin);

            double oldDistanceFare = 0.;
            if (timeBin2distanceFarePerMeter.get(timeBin) != null) {
                oldDistanceFare = timeBin2distanceFarePerMeter.get(timeBin);
            }

            double updatedDistanceFare = 0.;
            if (drtModeStats > optDrtConfigGroup.getModalSplitThresholdForFareAdjustment() * (1. + optDrtConfigGroup.getFluctuatingPercentage()))
                updatedDistanceFare = oldDistanceFare + optDrtConfigGroup.getFareAdjustment();
            else if (drtModeStats < optDrtConfigGroup.getModalSplitThresholdForFareAdjustment() * (1. - optDrtConfigGroup.getFluctuatingPercentage()))
                updatedDistanceFare = oldDistanceFare - optDrtConfigGroup.getFareAdjustment();
            else updatedDistanceFare = oldDistanceFare;

            // negative price should not be allowed.However, considering that the updated price is based on the original price.
            // Lower than the price defined in the drt-fare module should be taken into account.

            if (updatedDistanceFare < (0 - drtFareConfigGroup.distanceFare_m))
                updatedDistanceFare = 0 - drtFareConfigGroup.distanceFare_m;

            log.info("Fare in time bin " + timeBin + " changed from " + oldDistanceFare + " to " + updatedDistanceFare);

            timeBin2distanceFarePerMeter.put(timeBin, updatedDistanceFare);

        }


    }

    @Override
    public void writeInfo( int currentIteration ) {
            for (int i = 0; i < timeBin2DrtModalStats.size(); i++) {
                if (timeBin2totalTrips.get(i) == 0) {
                    timeBin2DrtModalStats.put(i, 0.);
                } else {
                    timeBin2DrtModalStats.put(i, timeBin2drtTrips.get(i) / timeBin2totalTrips.get(i));
                }
                log.info("-- mode share of drt at timeBin " + i + " = " + timeBin2DrtModalStats.get(i));
            }
            String runOutputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
            if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");

            int num = currentIteration;
            String path = runOutputDirectory +  "drtFare/";
            File dir = new File(path);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String fileName = path + this.scenario.getConfig().controler().getRunId() + "." + num + ".info_" + this.getClass().getName() + "_" + this.optDrtConfigGroup.getMode() + ".csv";
            File file = new File(fileName);

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                bw.write("time bin;time bin start time [sec];time bin end time [sec];totalTrips ;drtTrips ; drtModeStats ;fare [monetary units / meter]");
                bw.newLine();

                for (Integer timeBin : this.timeBin2DrtModalStats.keySet()) {

                    double timeBinStart = timeBin * optDrtConfigGroup.getFareTimeBinSize();
                    double timeBinEnd = timeBin * optDrtConfigGroup.getFareTimeBinSize() + optDrtConfigGroup.getFareTimeBinSize();

                    double fare = 0.;
                    if (this.timeBin2distanceFarePerMeter.get(timeBin) != null)
                        fare = drtFareConfigGroup.distanceFare_m + this.timeBin2distanceFarePerMeter.get(timeBin);

                    bw.write(String.valueOf(timeBin) + ";" + timeBinStart + ";" + timeBinEnd + ";" + this.timeBin2totalTrips.get(timeBin) + ";" + this.timeBin2drtTrips.get(timeBin) + ";" + this.timeBin2DrtModalStats.get(timeBin) + ";" + String.valueOf(fare));
                    bw.newLine();
                }
                log.info("Output written to " + fileName);
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (optDrtConfigGroup.getMode().equals(event.getLegMode())) {
            this.drtUserDepartureTime.put(event.getPersonId(), event.getTime());
        }

        if (this.personList.contains(event.getPersonId()) && this.mainTransportModes.contains(event.getLegMode())) {
            Map<Id<Person>, String> personId2Legmode = new HashMap<>();
            personId2Legmode.put(event.getPersonId(), event.getLegMode());
            this.personDepartureInfo.put(personId2Legmode, event.getTime());
        }

    }
}
