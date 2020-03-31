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
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
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
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;

/**
 * @author zmeng
 * <p>
 * An implementation for different DRT fares for different times of day.
 * The fares will be updated during the simulation depending on modestats.
 * <p>
 * Note that these fares are scored in excess to anything set in the modeparams in the config file or any other drt fare handler.
 */
public class OptDrtFareStrategyModalSplit implements PersonDepartureEventHandler, PersonArrivalEventHandler, OptDrtFareStrategy, DrtRequestSubmittedEventHandler {
    private static final Logger log = Logger.getLogger(OptDrtFareStrategyWaitingTime.class);

    private Map<Integer, Double> timeBin2distanceFarePerMeter = new HashMap<>();

    private Map<Id<Person>, DrtRequestSubmittedEvent> lastRequestSubmission = new HashMap<>();
    private Map<Id<Person>, Double> drtUserDepartureTime = new HashMap<>();
    private Map<Integer, Double> timeBin2DrtModalStats = new HashMap<>();
    private Map<Integer, Double> timeBin2totalTrips = new HashMap<>();
    private Map<Integer, Double> timeBin2drtTrips = new HashMap<>();
    private Map<Map<Id<Person>, String>, Double> personDepartureInfo = new HashMap<>();

    private Boolean updateFare;

    private List<String> mainTransportModes = new LinkedList<>();

    private Set<Id<Person>> personList;

    private int currentIteration;

    @Inject
    OptDrtConfigGroup optDrtConfigGroup;

    @Inject
    EventsManager events;

    @Inject
    Scenario scenario;

    @Inject
    MainModeIdentifier mainModeIdentifier;

    @Inject
    DrtFaresConfigGroup drtFaresConfigGroup;

    @Override
    public void handleEvent(PersonArrivalEvent event) {

        Map<Id<Person>, String> personId2Legmode = new HashMap<>();
        personId2Legmode.put(event.getPersonId(), event.getLegMode());

        if (this.personDepartureInfo.containsKey(personId2Legmode)) {
            int timeBin = getTimeBin(personDepartureInfo.get(personId2Legmode));
            this.timeBin2totalTrips.put(timeBin, timeBin2totalTrips.get(timeBin) + 1);
            this.personDepartureInfo.remove(personId2Legmode);
        }

        if (event.getLegMode().equals(optDrtConfigGroup.getOptDrtMode())) {

            DrtRequestSubmittedEvent e = this.lastRequestSubmission.get(event.getPersonId());

            int timeBin = getTimeBin(drtUserDepartureTime.get(event.getPersonId()));
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
                events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), ((-drtFareConfigGroup.getMinFarePerTrip())) + oldFare));
            } else {
                events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), -fare));
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
    public void reset(int iteration) {
        int timeBinSize = getTimeBin(scenario.getConfig().qsim().getEndTime().seconds());
        for (int timeBin = 0; timeBin <= timeBinSize; timeBin++) {
            this.timeBin2DrtModalStats.put(timeBin, 0.);
            this.timeBin2totalTrips.put(timeBin, 0.);
            this.timeBin2drtTrips.put(timeBin, 0.);
        }
        this.updateFare = false;
        lastRequestSubmission.clear();
        drtUserDepartureTime.clear();
        this.currentIteration = iteration;

        // 收集这个it里所有有效person的id
        this.personList = this.scenario.getPopulation().getPersons().keySet();
        log.info("-- active persons in " + this.currentIteration + ".it are " + Arrays.toString(personList.toArray()));

        // 收集这个it里所有的mainmode
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
        log.info("-- main mode in " + this.currentIteration + ".it are " + Arrays.toString(this.mainTransportModes.toArray()));
    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if (optDrtConfigGroup.getOptDrtMode().equals(event.getMode())) {
            this.lastRequestSubmission.put(event.getPersonId(), event);
        }
    }

    @Override
    public void updateFares() {
        this.updateFare = true;
        for (int i = 0; i < timeBin2DrtModalStats.size(); i++) {
            if (timeBin2totalTrips.get(i) == 0) {
                timeBin2DrtModalStats.put(i, 0.);
            } else {
                timeBin2DrtModalStats.put(i, timeBin2drtTrips.get(i) / timeBin2totalTrips.get(i));
            }
            log.info("-- mode share of drt at timeBin " + i + " = " + timeBin2DrtModalStats.get(i));
        }
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
            DrtFareConfigGroup drtFareConfigGroup = drtFaresConfigGroup.getDrtFareConfigGroups().stream().filter(drtFareConfigGroup1 -> drtFareConfigGroup1.getMode().equals("drt")).collect(Collectors.toList()).get(0);

            if (updatedDistanceFare < (0 - drtFareConfigGroup.getDistanceFare_m()))
                updatedDistanceFare = 0 - drtFareConfigGroup.getDistanceFare_m();

            log.info("Fare in time bin " + timeBin + " changed from " + oldDistanceFare + " to " + updatedDistanceFare);

            timeBin2distanceFarePerMeter.put(timeBin, updatedDistanceFare);

        }


    }

    @Override
    public void writeInfo() {
        if (!updateFare) {
            for (int i = 0; i < timeBin2DrtModalStats.size(); i++) {
                if (timeBin2totalTrips.get(i) == 0) {
                    timeBin2DrtModalStats.put(i, 0.);
                } else {
                    timeBin2DrtModalStats.put(i, timeBin2drtTrips.get(i) / timeBin2totalTrips.get(i));
                }
                log.info("-- mode share of drt at timeBin " + i + " = " + timeBin2DrtModalStats.get(i));
            }
        } else {
            String runOutputDirectory = this.scenario.getConfig().controler().getOutputDirectory();
            if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");

            int num = currentIteration;
            String path = runOutputDirectory +  "drtFare/";
            File dir = new File(path);
            if(!dir.exists()){
                dir.mkdirs();
            }
            String fileName = path + this.scenario.getConfig().controler().getRunId() + "." + num + ".info_" + this.getClass().getName() + ".csv";
            File file = new File(fileName);

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(file));

                bw.write("time bin;time bin start time [sec];time bin end time [sec];totalTrips ;drtTrips ; drtModeStats ;fare [monetary units / meter]");
                bw.newLine();

                for (Integer timeBin : this.timeBin2DrtModalStats.keySet()) {

                    double timeBinStart = timeBin * optDrtConfigGroup.getFareTimeBinSize();
                    double timeBinEnd = timeBin * optDrtConfigGroup.getFareTimeBinSize() + optDrtConfigGroup.getFareTimeBinSize();

                    DrtFareConfigGroup drtFareConfigGroup = drtFaresConfigGroup.getDrtFareConfigGroups().stream().filter(drtFareConfigGroup1 -> drtFareConfigGroup1.getMode().equals("drt")).collect(Collectors.toList()).get(0);

                    double fare = 0.;
                    if (this.timeBin2distanceFarePerMeter.get(timeBin) != null)
                        fare = drtFareConfigGroup.getDistanceFare_m() + this.timeBin2distanceFarePerMeter.get(timeBin);

                    bw.write(String.valueOf(timeBin) + ";" + timeBinStart + ";" + timeBinEnd + ";" + this.timeBin2totalTrips.get(timeBin) + ";" + this.timeBin2drtTrips.get(timeBin) + ";" + this.timeBin2DrtModalStats.get(timeBin) + ";" + String.valueOf(fare));
                    bw.newLine();
                }
                log.info("Output written to " + fileName);
                bw.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (optDrtConfigGroup.getOptDrtMode().equals(event.getLegMode())) {
            this.drtUserDepartureTime.put(event.getPersonId(), event.getTime());
        }

        if (this.personList.contains(event.getPersonId()) && this.mainTransportModes.contains(event.getLegMode())) {
            Map<Id<Person>, String> personId2Legmode = new HashMap<>();
            personId2Legmode.put(event.getPersonId(), event.getLegMode());
            this.personDepartureInfo.put(personId2Legmode, event.getTime());
        }

    }
}
