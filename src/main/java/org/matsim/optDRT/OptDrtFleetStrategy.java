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

import org.matsim.core.events.handler.EventHandler;

/**
 * @author ikaddoura
 */

public interface OptDrtFleetStrategy extends EventHandler {
	public void updateFleet( int currentIteration );

	public void writeInfo( int currentIteration );

	/**
	 * Separate this from the normal reset() method in ControlerListeners, because we have to ensure that last
	 * iterations' data is kept until updateFares() was run in the following iteration. But delete data immediately
	 * afterwards to not mix it up with the current iteration.
	 */
	public void resetDataForThisIteration( int currentIteration );
}

