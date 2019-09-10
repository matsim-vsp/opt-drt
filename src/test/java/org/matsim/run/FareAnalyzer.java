package org.matsim.run;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.core.api.internal.HasPersonId;

import java.util.HashMap;
import java.util.Map;

public class FareAnalyzer implements PersonMoneyEventHandler, DrtRequestSubmittedEventHandler {

    private Map<Id<Person>, Double> PersonId2UnsharedRideTime = new HashMap<>();

    @Override
    public void handleEvent(PersonMoneyEvent personMoneyEvent) {

    }

    @Override
    public void handleEvent(DrtRequestSubmittedEvent drtRequestSubmittedEvent) {

    }

    @Override
    public void reset(int iteration) {

    }
}
