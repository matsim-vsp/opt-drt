package org.matsim.run;

import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.matsim.testcases.MatsimTestUtils;

/**
 * 
 * @author ikaddoura
 *
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RunOptDrtEquilScenarioTest {
		
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public final void test1() {
		RunExampleOptDrt.main(null);
	}

}
