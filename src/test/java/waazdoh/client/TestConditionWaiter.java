package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.client.utils.ConditionWaiter;

public class TestConditionWaiter extends TestCase {

	public void testWait() {
		ConditionWaiter w = ConditionWaiter.wait(
				new ConditionWaiter.Condition() {

					@Override
					public boolean test() {
						return true;
					}
				}, -1);
		assertTrue(w.isDone());
	}
}
