package org.matsim.optDRT;

import org.matsim.core.controler.AllowsConfiguration;

public class OptDrt {

	public static void addAsOverridingModule(AllowsConfiguration controler,
			MultiModeOptDrtConfigGroup multiModeOptDrtConfigGroup) {
		controler.addOverridingModule(new MultiModeOptDrtModule());
		controler.addOverridingQSimModule(new MultiModeOptDrtQSimModule(multiModeOptDrtConfigGroup));
	}
}
