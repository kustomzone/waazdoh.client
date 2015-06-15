package waazdoh.client;

import waazdoh.client.utils.ConditionWaiter;
import waazdoh.client.utils.ConditionWaiter.Condition;
import waazdoh.client.utils.ThreadChecker;
import waazdoh.client.utils.ThreadChecker.IChecker;

public class TestUtils extends WCTestCase {

	public void testThreadChecker() {
		final long st = System.currentTimeMillis();
		final Condition c = new Condition() {
			@Override
			public boolean test() {
				return (System.currentTimeMillis() - st) > 2000;
			}
		};

		new ThreadChecker(new IChecker() {

			@Override
			public boolean check() {
				return !c.test();
			}
		}, 100);

		final ConditionWaiter w = new ConditionWaiter(c, 10000);
	}
}
