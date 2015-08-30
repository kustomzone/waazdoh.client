package waazdoh.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import org.xml.sax.SAXException;

import waazdoh.client.model.objects.Binary;
import waazdoh.client.model.objects.BinaryListener;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.client.utils.MCRC;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;

public final class TestBinary extends WCTestCase {
	private WLogger log = WLogger.getLogger(this);

	public void testBinary() throws IOException, SAXException {

		Binary binary = new Binary(getClient(getRandomUserName(), false),
				getTempPath(), "test", "test");
		addData(binary);

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
		Binary binary2 = new Binary(getClient(getName(), false), getTempPath(),
				"test2", "test");
		binary2.importStream(binary.getInputStream());

		assertEquals(crc, binary2.getCRC());

		assertBinariesAreSame(binary, binary2);
	}

	private void addData(Binary binary) throws IOException {
		for (int i = 0; i < 100000; i++) {
			byte byt = (byte) (i & 0xff);
			binary.add(Byte.valueOf(byt));
		}
	}

	private void assertBinariesAreSame(Binary binary, Binary binary2)
			throws IOException {
		byte[] bs1 = new byte[1000];
		byte[] bs2 = new byte[1000];

		InputStream is1 = binary.getInputStream();
		InputStream is2 = binary2.getInputStream();

		while (true) {
			int bcount = is2.read(bs2);
			int acount = is1.read(bs1);

			if (acount != bcount) {
				assertEquals(acount, bcount);
			}

			if (acount < 0) {
				break;
			} else {
				for (int i = 0; i < acount; i++) {
					assertEquals(bs1[i], bs2[i]);
				}
			}
		}
	}

	public void testAddBytes() throws IOException {
		Binary b = getNewBinary();
		byte bs[] = new byte[2000];
		for (int i = 0; i < bs.length; i++) {
			bs[i] = (byte) (i & 0xff);
		}
		b.add(bs);
		b.setReady();

		InputStream is = b.getInputStream();
		for (int i = 0; i < bs.length; i++) {
			byte ib = (byte) (is.read() & 0xff);
			assertEquals("index:" + i, bs[i], ib);
		}
	}

	public void testBinaryHash() throws IOException {
		Binary a = getNewBinary();
		byte[] bs = new byte[1000];
		a.add(bs, bs.length);

		assertNotNull(a.getCRC());

		Binary b = new Binary(a.getService(), getTempPath(), "test", "test");
		b.add(bs, bs.length);

		assertEquals(a.getCRC(), b.getCRC());
	}

	public void testBinaryBean() throws IOException {
		Binary a = getNewBinary();
		byte[] bs = new byte[1000];
		a.add(bs);
		assertNotNull(a.getID());
		Binary b = new Binary(a.getService(), getTempPath(), "test", "test");
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
		ConditionWaiter.wait(new ConditionWaiter.Condition() {
			@Override
			public boolean test() {
				return sb.length() > 0;
			}
		}, 10000);
		assertTrue(sb.length() > 0);
	}

	private Binary getNewBinary() {
		try {
			return new Binary(getClient(getRandomUserName(), false),
					getTempPath(), "test", "test");
		} catch (MalformedURLException | SAXException e) {
			log.error(e);
			return null;
		}
	}

	public void testLoadSharedFile() throws SAXException, IOException {
		WClient c1 = getClient(getRandomUserName(), false);
		Binary b1 = c1.getBinarySource().newBinary("test", "test");
		assertNotNull(b1);
		addData(b1);
		b1.setReady();
		b1.publish();
		//
		WPreferences p1 = c1.getPreferences();

		//
		WClient c2 = getClient(getRandomUserName(), false);
		c2.getPreferences().set(WPreferences.LOCAL_PATH,
				p1.get(WPreferences.LOCAL_PATH, "FAIL"));

		Binary b2 = c2.getBinarySource().getOrDownload(b1.getID());
		assertNotNull(b2);
		assertTrue(b2.checkCRC());
		assertEquals(b1.getCRC(), b2.getCRC());
		//
		assertNotNull(c2.getBinarySource().get(b1.getID()));
	}

	public void testLoad() throws IOException {
		Binary b1 = getNewBinary();
		addData(b1);
		b1.setReady();

		Binary b2 = getNewBinary();
		b2.load(b1.getInputStream());
		b2.setReady();

		assertEquals(b2.getCRC(), b1.getCRC());
	}
}
