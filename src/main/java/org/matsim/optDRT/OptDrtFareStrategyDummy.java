package org.matsim.optDRT;

import com.google.inject.Inject;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;

/**
 * This is a test class for fares' changing
 * @author zmeng
 */
public class OptDrtFareStrategyDummy implements OptDrtFareStrategy, PersonArrivalEventHandler {

    @Inject
    private EventsManager events;
    @Inject
    private OptDrtConfigGroup optDrtConfigGroup ;

    @Override
    public void updateFares() {}

    @Override
    public void writeInfo() {}

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(optDrtConfigGroup.getOptDrtMode())) {
            events.processEvent(new PersonMoneyEvent(event.getTime(), event.getPersonId(), optDrtConfigGroup.getFareAdjustment()));
        }
    }
}
