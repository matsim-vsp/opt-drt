/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.locationtech.jts.geom.Geometry;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.DefaultPassengerRequestValidator;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.PassengerRequestValidator;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;

import com.google.inject.Inject;

/**
* @author ikaddoura
*/

public class OptDrtServiceAreaStrategyDemand implements PassengerRequestValidator, OptDrtServiceAreaStrategy, PersonDepartureEventHandler, PersonArrivalEventHandler {
	private static final Logger log = Logger.getLogger(OptDrtServiceAreaStrategyDemand.class);

	public static final String FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE = "from_link_not_in_service_area";
	public static final String TO_LINK_NOT_IN_SERVICE_AREA_CAUSE = "to_link_not_in_service_area";

	private final DefaultPassengerRequestValidator delegate = new DefaultPassengerRequestValidator();
	private final Map<Integer, Geometry> geometries;

	private Map<Integer, Integer> currentServiceAreaGeometryIds2Demand = new HashMap<>();
	
	private final OptDrtConfigGroup optDrtCfg;
	
	@Inject
	private Scenario scenario;

	public OptDrtServiceAreaStrategyDemand(OptDrtConfigGroup optDrtCfg) {
		this.optDrtCfg = optDrtCfg;
		
		geometries = loadShapeFile(optDrtCfg.getInputShapeFileForServiceAreaAdjustment());
		
		// load intital service area
		final Map<Integer, Geometry> geometriesInitialServiceArea = loadShapeFile(optDrtCfg.getInputShapeFileInitialServiceArea());
		for (Integer key : geometries.keySet()) {
			if (isGeometryInArea(geometries.get(key), geometriesInitialServiceArea)) currentServiceAreaGeometryIds2Demand.put(key, 0);
		}
		
	}
	
	private boolean isGeometryInArea(Geometry geometry, Map<Integer, Geometry> areaGeometries) {
		boolean coordInArea = false;
		for (Geometry areaGeometry : areaGeometries.values()) {
			if (geometry.within(areaGeometry)) {
				coordInArea = true;
			}
		}
		return coordInArea;
	}

	private Map<Integer, Geometry> loadShapeFile(String shapeFile) {
		Map<Integer, Geometry> geometries = new HashMap<>();

		Collection<SimpleFeature> features = null;
		if (new File(shapeFile).exists()) {
			features = ShapeFileReader.getAllFeatures(shapeFile);	
		} else {
			try {
				features = getAllFeatures(new URL(shapeFile));
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
		if (features == null) throw new RuntimeException("Aborting...");
		int featureCounter = 0;
		for (SimpleFeature feature : features) {
			geometries.put(featureCounter, (Geometry) feature.getDefaultGeometry());
			featureCounter++;
		}
		return geometries;
	}
	
	private Collection<SimpleFeature> getAllFeatures(final URL url) {
		try {
			FileDataStore store = FileDataStoreFinder.getDataStore(url);
			SimpleFeatureSource featureSource = store.getFeatureSource();

			SimpleFeatureIterator it = featureSource.getFeatures().features();
			List<SimpleFeature> featureSet = new ArrayList<SimpleFeature>();
			while (it.hasNext()) {
				SimpleFeature ft = it.next();
				featureSet.add(ft);
			}
			it.close();
			store.dispose();
			return featureSet;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public Set<String> validateRequest(PassengerRequest request) {

		Set<String> invalidRequestCauses = new HashSet<>();

		invalidRequestCauses.addAll(this.delegate.validateRequest(request));

		boolean fromLinkInServiceArea = false;
		if (isLinkInCurrentServiceArea(request.getFromLink()) != null) {
			fromLinkInServiceArea = true;
		}
		boolean toLinkInServiceArea = false;
		if (isLinkInCurrentServiceArea(request.getToLink()) != null) {
			toLinkInServiceArea = true;
		}

		if (!fromLinkInServiceArea ) {
			invalidRequestCauses.add(FROM_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		if (!toLinkInServiceArea) {
			invalidRequestCauses.add(TO_LINK_NOT_IN_SERVICE_AREA_CAUSE);
		}
		
		return invalidRequestCauses;
	}

	private Integer isLinkInCurrentServiceArea(Link link) {
		for (Integer key : this.currentServiceAreaGeometryIds2Demand.keySet()) {
			if (MGC.coord2Point(link.getCoord()).within(this.geometries.get(key))) return key;
		}
		return null;
	}

	@Override
	public void updateServiceArea() {
		
		// reduce service area
		Set<Integer> geometriesToRemove = new HashSet<>();
		for (Integer key : this.currentServiceAreaGeometryIds2Demand.keySet()) {
			if (this.currentServiceAreaGeometryIds2Demand.get(key) < this.optDrtCfg.getDemandThresholdForServiceAreaAdjustment()) {
				geometriesToRemove.add(key);
			}
		}
		for (Integer key : geometriesToRemove) {
			log.info("Removing geometry " + key);
			this.currentServiceAreaGeometryIds2Demand.remove(key);
		}
		
		// expand service area
		List<Integer> nonServiceAreaKeys = new ArrayList<>();
		for (Integer key : this.geometries.keySet()) {
			if (this.currentServiceAreaGeometryIds2Demand.get(key) == null) {
				nonServiceAreaKeys.add(key);
			}
		}
		List<Integer> newServiceAreaKeys = new ArrayList<>();
		if (nonServiceAreaKeys.size() > 0) {
			for (int counter = 0; counter < this.optDrtCfg.getServiceAreaAdjustment(); counter++) {
				int randomNr = (int) (MatsimRandom.getLocalInstance().nextDouble() * nonServiceAreaKeys.size());
				Integer newKey = nonServiceAreaKeys.get(randomNr);
				newServiceAreaKeys.add(newKey);
				nonServiceAreaKeys.remove(newKey);
			}
			
		for (Integer key : newServiceAreaKeys) {
			this.currentServiceAreaGeometryIds2Demand.put(key, 0);
			log.info("Adding geometry " + key);
		}
			
		} else {
			log.info("Drt service area has reached maximum expansion. Cannot expand service area any further...");
		}
		
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (event.getLegMode().equals(this.optDrtCfg.getOptDrtMode())) {
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			Integer key = isLinkInCurrentServiceArea(link);
			if (isLinkInCurrentServiceArea(link) != null) {
				int demand = this.currentServiceAreaGeometryIds2Demand.get(key);
				this.currentServiceAreaGeometryIds2Demand.put(key, demand + 1);
			};
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (event.getLegMode().equals(this.optDrtCfg.getOptDrtMode())) {
			Link link = scenario.getNetwork().getLinks().get(event.getLinkId());
			Integer key = isLinkInCurrentServiceArea(link);
			if (isLinkInCurrentServiceArea(link) != null) {
				int demand = this.currentServiceAreaGeometryIds2Demand.get(key);
				this.currentServiceAreaGeometryIds2Demand.put(key, demand + 1);
			};
		}
	}

	@Override
	public void writeInfo() {
		// TODO
	}

}

