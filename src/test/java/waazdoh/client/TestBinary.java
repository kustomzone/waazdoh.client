package waazdoh.client;

import junit.framework.TestCase;
import waazdoh.cutils.MCRC;
import waazdoh.cutils.MLogger;
import waazdoh.test.StaticService;

public class TestBinary extends TestCase {
	private MLogger log = MLogger.getLogger(this);

	public void testBinary() {

		Binary binary = new Binary(new StaticService(), "test", "test");
		assertNotNull(binary.getCRC());
		//
		for (int i = 0; i < 100000; i++) {
			byte byt = (byte) (i & 0xff);
			binary.add(new Byte(byt));
		}
		//
		MCRC crc = binary.getCRC();
		assertNotNull(crc);
		//
		Binary binary2 = new Binary(new StaticService(), "test2", "test");
		binary2.add(binary.asByteBuffer());
		//
		assertEquals(crc, binary2.getCRC());
		//
		for (int i = 0; i < binary.length(); i++) {
			Byte b = binary2.get(i);
			Byte a = binary.get(i);
			if (a != b) {
				assertEquals(a, b);
			}
		}
	}
}
