package org.matsim.optDRT;

import org.matsim.core.controler.AllowsConfiguration;

public class OptDrt {

	public static void addAsOverridingModule(AllowsConfiguration controler) {
		controler.addOverridingModule(new MultiModeOptDrtModule());
//		controler.addOverridingQSimModule(new OptDrtQSimModule(optDrtConfigGroup));
	}
}
