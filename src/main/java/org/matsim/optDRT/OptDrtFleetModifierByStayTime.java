/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.optDRT;

import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.random.RandomGenerator;
import org.apache.commons.math3.random.Well19937c;
import org.apache.commons.math3.util.Pair;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskBaseType;
import org.matsim.contrib.drt.speedup.DrtSpeedUp;
import org.matsim.contrib.drt.speedup.DrtSpeedUpParams;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.DvrpVehicleSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskEndedEventHandler;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEvent;
import org.matsim.contrib.dvrp.vrpagent.TaskStartedEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author vsp-gleich
 */

class OptDrtFleetModifierByStayTime implements TaskStartedEventHandler, TaskEndedEventHandler, OptDrtFleetModifier,
        BeforeMobsimListener, IterationEndsListener {
    private static final Logger log = Logger.getLogger(OptDrtFleetModifierByStayTime.class);

    private final FleetSpecification fleetSpecification;

    private final OptDrtConfigGroup optDrtConfigGroup;

    private final ControlerConfigGroup controlerConfigGroup;

    private final RandomGenerator rng;
    private final Map<Id<DvrpVehicle>, Double> drtVehStayLastBeginTime = new HashMap<>();
    private final Map<Id<DvrpVehicle>, Double> drtVehStayTime = new HashMap<>();
    int vehicleCounter = 0;
    private final Optional<DrtSpeedUpParams> speedUpParams;

    private final Map<Id<DvrpVehicle>, Set<Id<DvrpVehicle>>> childClonesPerParent = new HashMap<>();


    public OptDrtFleetModifierByStayTime(FleetSpecification fleetSpecification,
                                         OptDrtConfigGroup optDrtConfigGroup, Config config) {
        this.fleetSpecification = fleetSpecification;
        this.optDrtConfigGroup = optDrtConfigGroup;
        this.controlerConfigGroup = config.controler();
        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule(config, MultiModeDrtConfigGroup.class);
        DrtConfigGroup drtConfigGroup = multiModeDrtConfigGroup.getModalElements().stream().filter(modal -> modal.getMode().equals(optDrtConfigGroup.getMode())).findAny().orElseThrow();
        this.speedUpParams = drtConfigGroup.getDrtSpeedUpParams();

        /*
         * Always use the same RandomNumberGenerator per instance, set seed to Matsim config specification.
         * This is a tricky decision. The RandomNumberGenerator should not be used by multiple threads at the same time.
         * This seems to be true here. Then explicitly setting the RandomNumberGenerator should help with
         * reproducibility.
         */
        this.rng = new Well19937c(config.global().getRandomSeed());
    }

    /**
     * Not useful because we use the data of the previous iteration at the start of the following iteration. So we want
     * to reset later than the reset() call. That's why BeforeMobsimListener is used here.
     *
     * @param iteration currently started iteration
     */
    @Override
    public void reset(int iteration) {
    }

    public final void decreaseFleet(int vehiclesToRemove) {

        log.info("Removing " + vehiclesToRemove + " vehicles using FleetUpdateVehicleSelection " +
                OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration +
                " with VehicleSelectionRandomnessConstant " + optDrtConfigGroup.getVehicleSelectionRandomnessConstant());
        EnumeratedDistribution<Id<DvrpVehicle>> weightedVehicles =
                getWeightedDistributionOfVehiclesForRemoving(optDrtConfigGroup.getVehicleSelectionRandomnessConstant());

        Set<Id<DvrpVehicle>> dvrpVehiclesToRemove = new HashSet<>();
        for (int v = 0; v < Math.min(vehiclesToRemove, fleetSpecification.getVehicleSpecifications().size() - 1); v++) {
            dvrpVehiclesToRemove.add(selectVehicleByWeightedDrawNotYetInSet(weightedVehicles, dvrpVehiclesToRemove));
        }

        for (Id<DvrpVehicle> id : dvrpVehiclesToRemove) {
            if (fleetSpecification.getVehicleSpecifications().size() > 1) {
                // always keep one drt 'mother' vehicle
                fleetSpecification.removeVehicleSpecification(id);
                log.info("Removing dvrp vehicle " + id);
            }
        }

        int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();

        log.info("Dvrp vehicle fleet was decreased to " + vehiclesAfter);
    }

    public final void increaseFleet(int vehiclesToAdd) {
        if (fleetSpecification.getVehicleSpecifications().keySet().size() < 1) {
            throw new RuntimeException("No dvrp vehicle found to be cloned. Maybe create some default dvrp vehicle which is specified somewhere. Aborting...");
        }
        log.info("Adding " + vehiclesToAdd + " vehicles using FleetUpdateVehicleSelection " +
                OptDrtConfigGroup.FleetUpdateVehicleSelection.WeightedRandomByDrtStayDuration +
                " with VehicleSelectionRandomnessConstant " + optDrtConfigGroup.getVehicleSelectionRandomnessConstant());
        EnumeratedDistribution<Id<DvrpVehicle>> weightedVehicles =
                getWeightedDistributionOfVehiclesForCloning(optDrtConfigGroup.getVehicleSelectionRandomnessConstant());
        for (int i = 0; i < vehiclesToAdd; i++) {
            cloneAndAddDvrpVehicle(fleetSpecification.getVehicleSpecifications().get(selectVehicleByWeightedDraw(weightedVehicles)));
        }

        int vehiclesAfter = fleetSpecification.getVehicleSpecifications().size();

        log.info("Dvrp vehicle fleet was increased to " + vehiclesAfter);

    }

    private void cloneAndAddDvrpVehicle(DvrpVehicleSpecification dvrpVehicleSpecficationToBeCloned) {
        Id<DvrpVehicle> idParent = dvrpVehicleSpecficationToBeCloned.getId();
        Id<DvrpVehicle> idChild = Id.create("optDrt_" + vehicleCounter + "_cloneOf_" + idParent, DvrpVehicle.class);
        DvrpVehicleSpecification newSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
                .id(idChild)
                .serviceBeginTime(dvrpVehicleSpecficationToBeCloned.getServiceBeginTime())
                .serviceEndTime(dvrpVehicleSpecficationToBeCloned.getServiceEndTime())
                .startLinkId(dvrpVehicleSpecficationToBeCloned.getStartLinkId())
                .capacity(dvrpVehicleSpecficationToBeCloned.getCapacity())
                .build();

        fleetSpecification.addVehicleSpecification(newSpecification);
        log.info("Adding dvrp vehicle " + idChild);

        vehicleCounter++;

        Set<Id<DvrpVehicle>> childSet = this.childClonesPerParent.getOrDefault(idParent, new HashSet<>());
        childSet.add(idChild);
        childClonesPerParent.put(idParent, childSet);

    }

    private EnumeratedDistribution<Id<DvrpVehicle>> getWeightedDistributionOfVehiclesForCloning(double constantToIncreaseRandomness) {
        List<Pair<Id<DvrpVehicle>, Double>> vehs2weights = new ArrayList<>();
        // weight by 1 / stay time -> busy vehicles have a higher weight
        // Math.max(1, value) -> avoid weight infinity
        drtVehStayTime.forEach((key, value) -> vehs2weights.add(new Pair<>(key, 1 / Math.max(1, value + constantToIncreaseRandomness))));
        return new EnumeratedDistribution<>(rng, vehs2weights);
    }

    private EnumeratedDistribution<Id<DvrpVehicle>> getWeightedDistributionOfVehiclesForRemoving(double constantToIncreaseRandomness) {
        List<Pair<Id<DvrpVehicle>, Double>> vehs2weights = new ArrayList<>();
        // weight by stay time -> idle vehicles have a higher weight
        // Math.max(1, value) -> vehicles without stay time have weight > 0
        drtVehStayTime.forEach((key, value) -> vehs2weights.add(new Pair<>(key, Math.max(1, value + constantToIncreaseRandomness))));
        return new EnumeratedDistribution<>(rng, vehs2weights);
    }

    private Id<DvrpVehicle> selectVehicleByWeightedDraw(EnumeratedDistribution<Id<DvrpVehicle>> distribution) {
        return distribution.sample();
    }

    private Id<DvrpVehicle> selectVehicleByWeightedDrawNotYetInSet(EnumeratedDistribution<Id<DvrpVehicle>> distribution, Set<Id<DvrpVehicle>> vehiclesToDelete) {
        Id<DvrpVehicle> candidate = distribution.sample();
        if (vehiclesToDelete.contains(candidate)) {
            return selectVehicleByWeightedDrawNotYetInSet(distribution, vehiclesToDelete);
        } else {
            return candidate;
        }
    }

    @Override
    public void handleEvent(TaskStartedEvent taskStartedEvent) {
        if (DrtTaskBaseType.STAY.isBaseTypeOf(taskStartedEvent.getTaskType()) && fleetSpecification.getVehicleSpecifications().keySet().contains(taskStartedEvent.getDvrpVehicleId())) {
            drtVehStayLastBeginTime.put(taskStartedEvent.getDvrpVehicleId(), taskStartedEvent.getTime());
        }
    }

    @Override
    public void handleEvent(TaskEndedEvent taskEndedEvent) {
        if (DrtTaskBaseType.STAY.isBaseTypeOf(taskEndedEvent.getTaskType()) && fleetSpecification.getVehicleSpecifications().keySet().contains(taskEndedEvent.getDvrpVehicleId())) {
            double startTime = drtVehStayLastBeginTime.get(taskEndedEvent.getDvrpVehicleId()); // TODO: cater for case not found/null ?
            assert taskEndedEvent.getTime() - startTime >= 0;
            drtVehStayTime.put(taskEndedEvent.getDvrpVehicleId(),
                    taskEndedEvent.getTime() - startTime + drtVehStayTime.getOrDefault(taskEndedEvent.getDvrpVehicleId(), 0.0));
        }
    }

    /**
     * Reset drt stay task listener before next mobsim begins. Reset cannot be done at IterationStartEvent or the usual
     * reset() method call to EventsListeners because that might be before the {@link OptDrtFleetStrategy} wants to
     * update the fleet based on <b>last</b> iteration's data.
     *
     * @param beforeMobsimEvent
     */
    @Override
    public void notifyBeforeMobsim(BeforeMobsimEvent beforeMobsimEvent) {
        // check if this iteration will be a speed up iteration, so we would rather want to keep the old data
        if (speedUpParams.isPresent() &&
                DrtSpeedUp.isTeleportDrtUsers(speedUpParams.orElseThrow(), controlerConfigGroup, beforeMobsimEvent.getIteration())) {
            return;
        } else {
            drtVehStayLastBeginTime.clear();
            drtVehStayTime.clear();
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent iterationEndsEvent) {
        String runOutputDirectory = this.controlerConfigGroup.getOutputDirectory();
        if (!runOutputDirectory.endsWith("/")) runOutputDirectory = runOutputDirectory.concat("/");

        int currentIteration = iterationEndsEvent.getIteration();

        String fileName = runOutputDirectory + "ITERS/it." + currentIteration + "/" + this.controlerConfigGroup.getRunId() + "." + currentIteration + ".info_" + this.getClass().getName() + "_" + this.optDrtConfigGroup.getMode() + ".csv";
        File file = new File(fileName);

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));

            bw.write("vehicle id;total stay time [sec];Number of Direct Children;Number of Indirect Children");
            bw.newLine();

            for (Id<DvrpVehicle> vehicleId : this.drtVehStayTime.keySet()) {
                Double stayTime = drtVehStayTime.get(vehicleId);
                int directChildren = childClonesPerParent.getOrDefault(vehicleId, new HashSet<>()).size();
                int indirectChildren = countIndirectChildren(vehicleId);

                bw.write(vehicleId.toString() + ";" + stayTime + ";" + directChildren + ";" + indirectChildren);
                bw.newLine();
            }
            log.info("Output written to " + fileName);
            bw.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * recursive method to count number of child clones for a given vehicle;
     * this includes not only the direct children, but the entire lineage (children of children of children...)
     */
    private int countIndirectChildren(Id<DvrpVehicle> veh) {

        // end of recursion; if vehicle is not parent to another clone, end of tree as been reached
        if (!childClonesPerParent.containsKey(veh)) {
            return 0;
        }

        int sum = 0;
        Set<Id<DvrpVehicle>> childrenOfVeh = childClonesPerParent.get(veh);
        for (Id<DvrpVehicle> child : childrenOfVeh) {
            sum = sum + 1 + countIndirectChildren(child);
        }

        return sum;

    }
}

