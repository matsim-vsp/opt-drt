package org.matsim.optDRT.analysis;

import com.google.inject.Provider;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;

import java.util.*;

import org.apache.log4j.Logger;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.optDRT.OptDrtConfigGroup;

import javax.inject.Inject;

public class DrtModeStatsControlerListener implements StartupListener, IterationEndsListener {
    private final Scenario scenario;
    private final Population population;
    private final ControlerConfigGroup controlerConfigGroup;
    private Map<Integer,Map<Integer,Double>> it2timeBin2drtModeStats = new HashMap<>();
    private int minIteration = 0;
    private final Provider<TripRouter> tripRouterFactory;
    private StageActivityTypes stageActivityTypes;
    private MainModeIdentifier mainModeIdentifier;
    private final OptDrtConfigGroup optDrtConfigGroup;
    private Map<Integer,Map<Integer,Integer>> it2timeBin2totalTrips = new HashMap<>();
    private Map<Integer,Map<Integer,Integer>> it2timeBin2drtTrips = new HashMap<>();
    private static final Logger log = Logger.getLogger(DrtModeStatsControlerListener.class);

    @Inject
    DrtModeStatsControlerListener(ControlerConfigGroup controlerConfigGroup, Population population1, Provider<TripRouter> tripRouterFactory, OptDrtConfigGroup optDrtConfigGroup, Scenario scenario) {
        this.controlerConfigGroup = controlerConfigGroup;
        this.population = population1;
        this.optDrtConfigGroup = optDrtConfigGroup;
        this.scenario = scenario;
        this.tripRouterFactory = tripRouterFactory;
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        this.collectDrtModeStatsInfo(iterationEndsEvent);
    }

    private void collectDrtModeStatsInfo(IterationEndsEvent iterationEndsEvent) {

        Map<Integer,Integer> timeBin2totalTrips = new HashMap<>();
        Map<Integer,Integer> timeBin2drtTrips = new HashMap<>();
        Map<Integer,Double> timeBin2drtModeStats = new HashMap<>();

        int timeBinSize =(int) (scenario.getConfig().qsim().getEndTime() / optDrtConfigGroup.getFareTimeBinSize());
        for (int timeBin = 0; timeBin <= timeBinSize ; timeBin++) {
            timeBin2drtModeStats.put(timeBin,0.);
            timeBin2totalTrips.put(timeBin,0);
            timeBin2drtTrips.put(timeBin,0);
        }

        List<TripStructureUtils.Trip> trips = new LinkedList<>();
        for (Person p :
                this.population.getPersons().values()) {
           trips.addAll(TripStructureUtils.getTrips(p.getSelectedPlan(),this.stageActivityTypes));
        }
        for (TripStructureUtils.Trip t :
                trips) {
            int a = (int) (t.getOriginActivity().getEndTime() / optDrtConfigGroup.getFareTimeBinSize());
            timeBin2totalTrips.put(a,(timeBin2totalTrips.get(a) + 1));
            if (this.mainModeIdentifier.identifyMainMode(t.getTripElements()).equals(TransportMode.drt)){
                timeBin2drtTrips.put(a,(timeBin2drtTrips.get(a) + 1));
            }
        }
        for (int i = 0; i < timeBin2drtModeStats.size() ; i++) {
            if( timeBin2totalTrips.get(i) == 0){
                timeBin2drtModeStats.put(i, 0.);
            } else {
                timeBin2drtModeStats.put(i, (double) (timeBin2drtTrips.get(i) / timeBin2totalTrips.get(i)));
            }
            log.info("-- mode share of drt at timeBin " + i + " = " + timeBin2drtModeStats.get(i));
        }
        this.it2timeBin2drtModeStats.put(iterationEndsEvent.getIteration(),timeBin2drtModeStats);
        this.it2timeBin2drtTrips.put(iterationEndsEvent.getIteration(),timeBin2drtTrips);
        this.it2timeBin2totalTrips.put(iterationEndsEvent.getIteration(),timeBin2totalTrips);
    }

    @Override
    public void notifyStartup(StartupEvent startupEvent) {
        this.minIteration = this.controlerConfigGroup.getFirstIteration();
        TripRouter tripRouter = (TripRouter) this.tripRouterFactory.get();
        this.stageActivityTypes = tripRouter.getStageActivityTypes();
        this.mainModeIdentifier = tripRouter.getMainModeIdentifier();
    }

    public Map<Integer, Map<Integer, Double>> getIt2TimeBin2drtModeStats() {
        return it2timeBin2drtModeStats;
    }

    public Map<Integer, Map<Integer, Integer>> getIt2TimeBin2totalTrips() {
        return it2timeBin2totalTrips;
    }

    public Map<Integer, Map<Integer, Integer>> getIt2TimeBin2drtTrips() {
        return it2timeBin2drtTrips;
    }
}
