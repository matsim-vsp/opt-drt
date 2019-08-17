package org.matsim.optDRT;

import org.matsim.core.controler.AllowsConfiguration;

public class OptDrt {

	public static void addAsOverridingModule(AllowsConfiguration controler, OptDrtConfigGroup optDrtConfigGroup) {
		controler.addOverridingModule(new OptDrtModule());
		controler.addOverridingQSimModule(new OptDrtQSimModule(optDrtConfigGroup.getOptDrtMode(), optDrtConfigGroup));
	}
}
