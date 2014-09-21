package waazdoh.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import junit.framework.TestCase;
import waazdoh.client.model.Binary;
import waazdoh.client.model.BinaryListener;
import waazdoh.testing.StaticService;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MCRC;
import waazdoh.util.MLogger;

public final class TestBinary extends TestCase {
	private MLogger log = MLogger.getLogger(this);

	public void testBinary() throws IOException {
		Binary binary = new Binary(new StaticService(), "test", "test");
		assertNotNull(binary.getCRC());
		for (int i = 0; i < 100000; i++) {
			byte byt = (byte) (i & 0xff);
			binary.add(Byte.valueOf(byt));
		}
		MCRC fcrc = binary.getCRC();
		// More bytes in chunks
		for (int count = 0; count < 100; count++) {
			for (int i = 0; i < 100; i++) {
				byte byt = (byte) (i & 0xff);
				binary.add(Byte.valueOf(byt));
			}
		}
		binary.setReady();
		assertEquals(110000, binary.length());
		MCRC crc = binary.getCRC();
		assertNotNull(crc);
		assertNotSame(fcrc, crc);
		// second binary read with inputstream.
		Binary binary2 = new Binary(new StaticService(), "test2", "test");
		binary2.importStream(new ByteArrayInputStream(binary.asByteBuffer()));

		assertEquals(crc, binary2.getCRC());
		for (int i = 0; i < binary.length(); i++) {
			Byte b = binary2.get(i);
			Byte a = binary.get(i);
			if (a != b) {
				assertEquals(a, b);
			}
		}
	}

	public void testAddBytes() {
		Binary b = getNewBinary();
		byte bs[] = new byte[2000];
		for (int i = 0; i < bs.length; i++) {
			bs[i] = (byte) (i & 0xff);
		}
		b.add(bs, 1500);
		assertEquals(1500, b.getBytesLength());
		for (int i = 0; i < b.getBytesLength(); i++) {
			assertTrue(b.getByteBuffer()[i] == bs[i]);
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
