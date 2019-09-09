package org.matsim.run;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonMoneyEventHandler;

public class FaresAnalyzer implements PersonDepartureEventHandler, PersonMoneyEventHandler {

    @Override
    public void handleEvent(PersonDepartureEvent event) {

    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {

    }

    @Override
    public void reset(int iteration) {

    }
}
