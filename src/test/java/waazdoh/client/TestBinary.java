package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.client.model.Binary;
import waazdoh.client.model.BinaryListener;
import waazdoh.testing.StaticService;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MCRC;
import waazdoh.util.MLogger;

public final class TestBinary extends TestCase {
	private MLogger log = MLogger.getLogger(this);

	public void testBinary() {
		Binary binary = new Binary(new StaticService(), "test", "test");
		assertNotNull(binary.getCRC());
		for (int i = 0; i < 100000; i++) {
			byte byt = (byte) (i & 0xff);
			binary.add(Byte.valueOf(byt));
		}
		MCRC crc = binary.getCRC();
		assertNotNull(crc);
		Binary binary2 = new Binary(new StaticService(), "test2", "test");
		binary2.add(binary.asByteBuffer());
		assertEquals(crc, binary2.getCRC());
		for (int i = 0; i < binary.length(); i++) {
			Byte b = binary2.get(i);
			Byte a = binary.get(i);
			if (a != b) {
				assertEquals(a, b);
			}
		}
	}

	public void testBinaryHash() {
		Binary a = getNewBinary();
		byte[] bs = new byte[1000];
		a.add(bs);
		assertNotNull(a.getCRC());
		Binary b = new Binary(a.getService(), "test", "test");
		b.add(bs);

		assertEquals(a.getCRC(), b.getCRC());
	}

	public void testBinaryBean() {
		Binary a = getNewBinary();
		byte[] bs = new byte[1000];
		a.add(bs);
		assertNotNull(a.getID());
		Binary b = new Binary(a.getService(), "test", "test");
		b.add(bs);

		assertEquals(a.getBean().toText(), b.getBean().toText());
	}

	public void testListener() {
		Binary a = getNewBinary();
		final StringBuilder sb = new StringBuilder();
		a.addListener(new BinaryListener() {

			@Override
			public void ready(Binary binary) {
				sb.append("ready");
			}
		});
		//
		a.setReady();
		new ConditionWaiter(new ConditionWaiter.Condition() {
			@Override
			public boolean test() {
				return sb.length() > 0;
			}
		}, 10000);
		assertTrue(sb.length() > 0);
	}

	private Binary getNewBinary() {
		StaticService service = new StaticService();
		return new Binary(service, "test", "test");

	}
}
