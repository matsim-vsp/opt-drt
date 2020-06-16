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

import org.apache.log4j.Logger;
import org.matsim.contrib.dvrp.run.Modal;
import org.matsim.core.config.Config;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author ikaddoura
 */

public class OptDrtConfigGroup extends ReflectiveConfigGroup implements Modal {
	private static final Logger log = Logger.getLogger(OptDrtConfigGroup.class);

	public static final String GROUP_NAME = "optDrt";

	private static final String OPT_DRT_MODE = "optDrtMode";

	private static final String UPDATE_INTERVAL = "optDrtUpdateInterval";
	private static final String UPDATE_END_FRACTION_ITERATION = "optDrtUpdateEndFractionIteration";
	private static final String WRITE_INFO_INTERVAL = "optDrtWriteInfoInterval";

	private static final String FARE_ADJUSTMENT = "fareAdjustment";
	private static final String FARE_ADJUSTMENT_APPROACH = "fareAdjustmentApproach";
	private static final String FARE_UPDATE_APPROACH = "fareUpdateApproach";
	private static final String WAITING_TIME_THRESHOLD_FOR_FARE_ADJUSTMENT = "fareAdjustmentWaitingTimeThreshold";
	private static final String MODAL_SPLIT_THRESHOLD_FOR_FARE_ADJUSTMENT = "fareAdjustmentModalSplitThreshold";
	private static final String FARE_TIME_BIN_SIZE = "fareTimeBinSize";
	private static final String TRIP_SHARE_FOR_FARE_ADJUSTMENT = "tripShareThresholdForFareAdjustment";
	private static final String FARE_ADJUSTMENT_COST_PER_VEHICLE_PER_SECOND = "costPerVehiclePerSecondFareAdjustment";
	private static final String FLUCTUATING_PERCENTAGE = "fluctuatingPercentage";

	private static final String FLEETSIZE_ADJUSTMENT = "fleetSizeAdjustment";
	private static final String FLEETSIZE_ADJUSTMENT_PERCENTAGE = "fleetSizeAdjustmentPercentage";
	private static final String FLEETSIZE_ADJUSTMENT_APPROACH = "fleetSizeAdjustmentApproach";
	private static final String PROFIT_THRESHOLD_FOR_FLEETSIZE_ADJUSTMENT = "fleetSizeAdjustmentProfitThreshold";
	private static final String COST_PER_VEHICLE_PER_DAY_FOR_FLEET_ADJUSTMENT = "fleetSizeAdjustmentCostPerVehPerDay";
	private static final String COST_PER_VEHICLE_PER_METER_FOR_FLEET_ADJUSTMENT = "fleetSizeAdjustmentCostPerVehPerMeter";
	private static final String WAITING_TIME_THRESHOLD_FOR_FLEET_ADJUSTMENT = "fleetSizeAdjustmentWaitingTimeThreshold";
	private static final String FlEETSIZE_TIME_BIN_SIZE = "fleetSizeTimeBinSize";
	private static final String TRIP_SHARE_FOR_FLEET_SIZE_ADJUSTMENT = "tripShareThresholdForFleetSizeAdjustment";
	
	private static final String SERVICE_AREA_ADJUSTMENT_EXPAND = "serviceAreaAdjustmentExpand";
	private static final String SERVICE_AREA_ADJUSTMENT_REDUCE = "serviceAreaAdjustmentReduce";
	private static final String SERVICE_AREA_ADJUSTMENT_APPROACH = "serviceAreaAdjustmentApproach";
	private static final String INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT = "serviceAreaAdjustmentInputShapeFile";
	private static final String INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT_CRS = "serviceAreaAdjustmentInputShapeFileCRS";
	private static final String INPUT_SHAPEFILE_INITIAL_SERVICE_AREA = "serviceAreaAdjustmentInputShapeFileInitialServiceArea";
	private static final String INPUT_SHAPEFILE_INITIAL_SERVICE_AREA_CRS = "serviceAreaAdjustmentInputShapeFileInitialServiceAreaCRS";
	private static final String DEMAND_THRESHOLD_FOR_SERVICE_AREA_ADJUSTMENT = "serviceAreaAdjustmentDemandThreshold";

	public OptDrtConfigGroup() {
		super(GROUP_NAME);
	}
	
	private String optDrtMode = "drt";
	private int updateInterval = 1;
	private double updateEndFractionIteration = 0.8;
	private int writeInfoInterval = 1;

	// fare
	private FareAdjustmentApproach fareAdjustmentApproach = FareAdjustmentApproach.AverageWaitingTimeThreshold;
	private FareUpdateApproach fareUpdateApproach = FareUpdateApproach.BangBang;
	private double fareAdjustment = 0.5;
	private double fareTimeBinSize = 900.;
	// waitingTime approach
	private double waitingTimeThresholdForFareAdjustment = 600.;
	private double tripShareThresholdForFareAdjustment = 0.90;
	// modeSplit approach
	private double 	modeSplitThresholdForFareAdjustment = 0.2;
	private double fluctuatingPercentage = 0.05;

	// fleet size
	private FleetSizeAdjustmentApproach fleetSizeAdjustmentApproach = FleetSizeAdjustmentApproach.AverageWaitingTimeThreshold;
	private int fleetSizeAdjustment = 1;
	private double fleetSizeAdjustmentPercentage = 0.;
	// profit approach
	private double profitThresholdForFleetSizeAdjustment = 0.;
	private double costPerVehPerDayForFleetAdjustment = 5.3;
	private double costPerVehPerMeterForFleetAdjustment = 0.35 / 1000.;
	private double costPerVehiclePerSecondFareAdjustment = 1. /3600. ;
	// waiting time approach
	private double fleetSizeTimeBinSize = 900.;
	private double waitingTimeThresholdForFleetSizeAdjustment = 600.;
	private double tripShareThresholdForFleetSizeAdjustment = 0.90;

	// service area
	private ServiceAreaAdjustmentApproach serviceAreaAdjustmentApproach = ServiceAreaAdjustmentApproach.Disabled;
	private String inputShapeFileForServiceAreaAdjustment = null;
	private String inputShapeFileForServiceAreaAdjustmentCrs = null;
	private String inputShapeFileInitialServiceArea = null;
	private String inputShapeFileInitialServiceAreaCrs = null;
	private int serviceAreaAdjustmentExpand = 1;
	private int serviceAreaAdjustmentReduce = 1;
	private int demandThresholdForServiceAreaAdjustment = 1;

	public enum FareAdjustmentApproach {
		Disabled, AverageWaitingTimeThreshold, WaitingTimePercentileThreshold, ModeSplitThreshold
	}
	
	public enum FareUpdateApproach {
		BangBang, Proportional, ProportionalWithMSA, SimpleOffset, BangBangWithMSA
	}

	public enum ServiceAreaAdjustmentApproach {
		Disabled, DemandThreshold
	}

	public enum FleetSizeAdjustmentApproach {
		Disabled, ProfitThreshold, AverageWaitingTimeThreshold, WaitingTimeThreshold
	}

	@Override
	protected void checkConsistency(Config config) {
		super.checkConsistency(config);

		if (getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled) {
			if (getInputShapeFileForServiceAreaAdjustment() == null
					|| getInputShapeFileForServiceAreaAdjustment().equals("")
					|| getInputShapeFileForServiceAreaAdjustment().equals("null")) {
				throw new RuntimeException(
						"opt drt input shape file for service area adjustment is 'null'. Aborting...");
			}
		}

		if (getServiceAreaAdjustmentApproach() != ServiceAreaAdjustmentApproach.Disabled) {
			if (getInputShapeFileInitialServiceArea() == null
					|| getInputShapeFileInitialServiceArea().equals("null")
					|| getInputShapeFileInitialServiceArea().equals("")) {
				log.info(
						"opt drt input shape file for initial service area is empty. Starting without any restriction regarding the drt service area...");
			}
		}
	}

	@StringGetter(FLUCTUATING_PERCENTAGE)
	public double getFluctuatingPercentage() {
		return fluctuatingPercentage;
	}

	@StringSetter(FLUCTUATING_PERCENTAGE)
	public void setFluctuatingPercentage(double fluctuatingPercentage) {
		this.fluctuatingPercentage = fluctuatingPercentage;
	}

	@StringGetter(FARE_ADJUSTMENT_COST_PER_VEHICLE_PER_SECOND)
	public double getCostPerVehiclePerSecondFareAdjustment() {
		return costPerVehiclePerSecondFareAdjustment;
	}

	@StringSetter(FARE_ADJUSTMENT_COST_PER_VEHICLE_PER_SECOND)
	public void setCostPerVehiclePerSecondFareAdjustment(double costPerVehiclePerSecondFareAdjustment) {
		this.costPerVehiclePerSecondFareAdjustment = costPerVehiclePerSecondFareAdjustment;
	}

	@StringGetter( DEMAND_THRESHOLD_FOR_SERVICE_AREA_ADJUSTMENT )
	public int getDemandThresholdForServiceAreaAdjustment() {
		return demandThresholdForServiceAreaAdjustment;
	}

	@StringSetter( DEMAND_THRESHOLD_FOR_SERVICE_AREA_ADJUSTMENT )
	public void setDemandThresholdForServiceAreaAdjustment(int demandThresholdForServiceAreaAdjustment) {
		this.demandThresholdForServiceAreaAdjustment = demandThresholdForServiceAreaAdjustment;
	}
	@StringGetter( MODAL_SPLIT_THRESHOLD_FOR_FARE_ADJUSTMENT )
	public double getModalSplitThresholdForFareAdjustment() {
		return modeSplitThresholdForFareAdjustment;
	}
	@StringSetter( MODAL_SPLIT_THRESHOLD_FOR_FARE_ADJUSTMENT )
	public void setModeSplitThresholdForFareAdjustment(double modeSplitThresholdForFareAdjustment) {
		this.modeSplitThresholdForFareAdjustment = modeSplitThresholdForFareAdjustment;
	}

	@StringGetter( SERVICE_AREA_ADJUSTMENT_EXPAND )
	public int getServiceAreaAdjustmentExpand() {
		return serviceAreaAdjustmentExpand;
	}

	@StringSetter( SERVICE_AREA_ADJUSTMENT_EXPAND )
	public void setServiceAreaAdjustmentExpand(int serviceAreaAdjustment) {
		this.serviceAreaAdjustmentExpand = serviceAreaAdjustment;
	}
	
	@StringGetter( SERVICE_AREA_ADJUSTMENT_REDUCE )
	public int getServiceAreaAdjustmentReduce() {
		return serviceAreaAdjustmentReduce;
	}

	@StringSetter( SERVICE_AREA_ADJUSTMENT_REDUCE )
	public void setServiceAreaAdjustmentReduce(int serviceAreaAdjustment) {
		this.serviceAreaAdjustmentReduce = serviceAreaAdjustment;
	}

	@StringSetter( OPT_DRT_MODE )
	public void setOptDrtMode(String optDrtMode) {
		this.optDrtMode = optDrtMode;
	}

	@StringGetter( FARE_ADJUSTMENT )
	public double getFareAdjustment() {
		return fareAdjustment;
	}

	@StringSetter( FARE_ADJUSTMENT )
	public void setFareAdjustment(double fareAdjustment) {
		this.fareAdjustment = fareAdjustment;
	}

	@StringGetter( UPDATE_INTERVAL )
	public int getUpdateInterval() {
		return updateInterval;
	}
	
	@StringSetter( UPDATE_INTERVAL )
	public void setUpdateInterval(int uPDATE_INTERVAL) {
		updateInterval = uPDATE_INTERVAL;
	}

	@StringGetter( UPDATE_END_FRACTION_ITERATION )
	public double getUpdateEndFractionIteration() {
		return updateEndFractionIteration;
	}

	@StringSetter( UPDATE_END_FRACTION_ITERATION )
	public void setUpdateEndIterationFraction(double updateEndFractionIteration) {
		this.updateEndFractionIteration = updateEndFractionIteration;
	}

	@StringGetter( FARE_TIME_BIN_SIZE )
	public double getFareTimeBinSize() {
		return fareTimeBinSize;
	}

	@StringSetter( FARE_TIME_BIN_SIZE )
	public void setFareTimeBinSize(double fareTimeBinSize) {
		this.fareTimeBinSize = fareTimeBinSize;
	}

	@StringGetter( WAITING_TIME_THRESHOLD_FOR_FARE_ADJUSTMENT )
	public double getWaitingTimeThresholdForFareAdjustment() {
		return waitingTimeThresholdForFareAdjustment;
	}

	@StringSetter( WAITING_TIME_THRESHOLD_FOR_FARE_ADJUSTMENT )
	public void setWaitingTimeThresholdForFareAdjustment(double waitingTimeThresholdForFareAdjustment) {
		this.waitingTimeThresholdForFareAdjustment = waitingTimeThresholdForFareAdjustment;
	}

	@StringGetter( FLEETSIZE_ADJUSTMENT )
	public int getFleetSizeAdjustment() {
		return fleetSizeAdjustment;
	}

	@StringSetter( FLEETSIZE_ADJUSTMENT )
	public void setFleetSizeAdjustment(int fleetSizeAdjustment) {
		this.fleetSizeAdjustment = fleetSizeAdjustment;
	}

	@StringGetter( PROFIT_THRESHOLD_FOR_FLEETSIZE_ADJUSTMENT )
	public double getProfitThresholdForFleetSizeAdjustment() {
		return profitThresholdForFleetSizeAdjustment;
	}

	@StringSetter( PROFIT_THRESHOLD_FOR_FLEETSIZE_ADJUSTMENT )
	public void setProfitThresholdForFleetSizeAdjustment(double profitThresholdForFleetSizeAdjustment) {
		this.profitThresholdForFleetSizeAdjustment = profitThresholdForFleetSizeAdjustment;
	}

	@StringGetter( COST_PER_VEHICLE_PER_DAY_FOR_FLEET_ADJUSTMENT )
	public double getCostPerVehPerDayForFleetAdjustment() {
		return costPerVehPerDayForFleetAdjustment;
	}

	@StringSetter( COST_PER_VEHICLE_PER_DAY_FOR_FLEET_ADJUSTMENT )
	public void setCostPerVehPerDayForFleetAdjustment(double costPerVehPerDayForFleetAdjustment) {
		this.costPerVehPerDayForFleetAdjustment = costPerVehPerDayForFleetAdjustment;
	}

	@StringGetter( COST_PER_VEHICLE_PER_METER_FOR_FLEET_ADJUSTMENT )
	public double getCostPerVehPerMeterForFleetAdjustment() {
		return costPerVehPerMeterForFleetAdjustment;
	}

	@StringSetter( COST_PER_VEHICLE_PER_METER_FOR_FLEET_ADJUSTMENT )
	public void setCostPerVehPerMeterForFleetAdjustment(double costPerVehPerMeterForFleetAdjustment) {
		this.costPerVehPerMeterForFleetAdjustment = costPerVehPerMeterForFleetAdjustment;
	}

	@StringGetter( INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT )
	public String getInputShapeFileForServiceAreaAdjustment() {
		return inputShapeFileForServiceAreaAdjustment;
	}

	@StringSetter( INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT )
	public void setInputShapeFileForServiceAreaAdjustment(String inputShapeFileForServiceAreaAdjustment) {
		this.inputShapeFileForServiceAreaAdjustment = inputShapeFileForServiceAreaAdjustment;
	}

	@StringGetter( INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT_CRS )
	public String getInputShapeFileForServiceAreaAdjustmentCrs() {
		return inputShapeFileForServiceAreaAdjustmentCrs;
	}

	@StringSetter( INPUT_SHAPEFILE_FOR_SERVICE_AREA_ADJUSTMENT_CRS )
	public void setInputShapeFileForServiceAreaAdjustmentCrs(String inputShapeFileForServiceAreaAdjustmentCrs) {
		this.inputShapeFileForServiceAreaAdjustmentCrs = inputShapeFileForServiceAreaAdjustmentCrs;
	}
	
	@StringGetter( INPUT_SHAPEFILE_INITIAL_SERVICE_AREA )
	public String getInputShapeFileInitialServiceArea() {
		return inputShapeFileInitialServiceArea;
	}

	@StringSetter( INPUT_SHAPEFILE_INITIAL_SERVICE_AREA )
	public void setInputShapeFileInitialServiceArea(String inputShapeFileInitialServiceArea) {
		this.inputShapeFileInitialServiceArea = inputShapeFileInitialServiceArea;
	}

	@StringGetter( INPUT_SHAPEFILE_INITIAL_SERVICE_AREA_CRS )
	public String getInputShapeFileInitialServiceAreaCrs() {
		return inputShapeFileInitialServiceAreaCrs;
	}

	@StringSetter( INPUT_SHAPEFILE_INITIAL_SERVICE_AREA_CRS )
	public void setInputShapeFileInitialServiceAreaCrs(String inputShapeFileInitialServiceAreaCrs) {
		this.inputShapeFileInitialServiceAreaCrs = inputShapeFileInitialServiceAreaCrs;
	}

	@StringGetter( FARE_ADJUSTMENT_APPROACH )
	public FareAdjustmentApproach getFareAdjustmentApproach() {
		return fareAdjustmentApproach;
	}

	@StringSetter( FARE_ADJUSTMENT_APPROACH )
	public void setFareAdjustmentApproach(FareAdjustmentApproach fareAdjustmentApproach) {
		this.fareAdjustmentApproach = fareAdjustmentApproach;
	}

	@StringGetter( FLEETSIZE_ADJUSTMENT_APPROACH )
	public FleetSizeAdjustmentApproach getFleetSizeAdjustmentApproach() {
		return fleetSizeAdjustmentApproach;
	}

	@StringSetter( FLEETSIZE_ADJUSTMENT_APPROACH )
	public void setFleetSizeAdjustmentApproach(FleetSizeAdjustmentApproach fleetSizeAdjustmentApproach) {
		this.fleetSizeAdjustmentApproach = fleetSizeAdjustmentApproach;
	}

	@StringGetter( WAITING_TIME_THRESHOLD_FOR_FLEET_ADJUSTMENT )
	public double getWaitingTimeThresholdForFleetSizeAdjustment() {
		return waitingTimeThresholdForFleetSizeAdjustment;
	}

	@StringSetter( WAITING_TIME_THRESHOLD_FOR_FLEET_ADJUSTMENT )
	public void setWaitingTimeThresholdForFleetSizeAdjustment(double waitingTimeThresholdForFleetSizeAdjustment) {
		this.waitingTimeThresholdForFleetSizeAdjustment = waitingTimeThresholdForFleetSizeAdjustment;
	}

	@StringGetter( SERVICE_AREA_ADJUSTMENT_APPROACH )
	public ServiceAreaAdjustmentApproach getServiceAreaAdjustmentApproach() {
		return serviceAreaAdjustmentApproach;
	}

	@StringSetter( SERVICE_AREA_ADJUSTMENT_APPROACH )
	public void setServiceAreaAdjustmentApproach(ServiceAreaAdjustmentApproach serviceAreaAdjustmentApproach) {
		this.serviceAreaAdjustmentApproach = serviceAreaAdjustmentApproach;
	}

	@StringGetter( FlEETSIZE_TIME_BIN_SIZE )
	public double getFleetSizeTimeBinSize() {
		return fleetSizeTimeBinSize;
	}

	@StringSetter( FlEETSIZE_TIME_BIN_SIZE )
	public void setFleetSizeTimeBinSize(double fleetSizeTimeBinSize) {
		this.fleetSizeTimeBinSize = fleetSizeTimeBinSize;
	}

	@StringGetter( WRITE_INFO_INTERVAL )
	public int getWriteInfoInterval() {
		return writeInfoInterval;
	}

	@StringSetter( WRITE_INFO_INTERVAL )
	public void setWriteInfoInterval(int writeInfoInterval) {
		this.writeInfoInterval = writeInfoInterval;
	}
	
	@StringGetter( FARE_UPDATE_APPROACH )
	public FareUpdateApproach getFareUpdateApproach() {
		return fareUpdateApproach;
	}
	
	@StringSetter( FARE_UPDATE_APPROACH )
	public void setFareUpdateApproach(FareUpdateApproach fareUpdateApproach) {
		this.fareUpdateApproach = fareUpdateApproach;
	}
	@StringGetter( TRIP_SHARE_FOR_FLEET_SIZE_ADJUSTMENT )
	public double getTripShareThresholdForFleetSizeAdjustment() {
		return tripShareThresholdForFleetSizeAdjustment;
	}
	@StringSetter( TRIP_SHARE_FOR_FLEET_SIZE_ADJUSTMENT )
	public void setTripShareThresholdForFleetSizeAdjustment(double tripShareThresholdForFleetSizeAdjustment) {
		this.tripShareThresholdForFleetSizeAdjustment = tripShareThresholdForFleetSizeAdjustment;
	}
	@StringGetter( FLEETSIZE_ADJUSTMENT_PERCENTAGE )
	public double getFleetSizeAdjustmentPercentage() {
		return fleetSizeAdjustmentPercentage;
	}
	@StringSetter( FLEETSIZE_ADJUSTMENT_PERCENTAGE )
	public void setFleetSizeAdjustmentPercentage(double fleetSizeAdjustmentPercentage) {
		this.fleetSizeAdjustmentPercentage = fleetSizeAdjustmentPercentage;
	}
	@StringGetter( TRIP_SHARE_FOR_FARE_ADJUSTMENT )
	public double getTripShareThresholdForFareAdjustment() {
		return tripShareThresholdForFareAdjustment;
	}
	@StringSetter( TRIP_SHARE_FOR_FARE_ADJUSTMENT )
	public void setTripShareThresholdForFareAdjustment(double tripShareThresholdForFareAdjustment) {
		this.tripShareThresholdForFareAdjustment = tripShareThresholdForFareAdjustment;
	}
	@Override
	@StringGetter(OPT_DRT_MODE)
	public String getMode() {
		return optDrtMode;
	}
			
}

