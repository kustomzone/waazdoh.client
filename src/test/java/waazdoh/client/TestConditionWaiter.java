package waazdoh.client;

import waazdoh.client.utils.ConditionWaiter;
import junit.framework.TestCase;

public class TestConditionWaiter extends TestCase {

	public void testWait() {
		ConditionWaiter w = new ConditionWaiter(
				new ConditionWaiter.Condition() {

					@Override
					public boolean test() {
						return true;
					}
				}, -1);
		assertTrue(w.isDone());
	}
}
