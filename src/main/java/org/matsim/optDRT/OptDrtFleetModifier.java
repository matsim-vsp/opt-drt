package org.matsim.optDRT;

import org.matsim.core.controler.listener.ControlerListener;
import org.matsim.core.events.handler.EventHandler;

public interface OptDrtFleetModifier extends EventHandler, ControlerListener {

    void increaseFleet(int vehiclesToAdd);

    void decreaseFleet(int vehiclesToRemove);
}
