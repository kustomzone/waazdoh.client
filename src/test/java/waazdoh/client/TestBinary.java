package waazdoh.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.xml.sax.SAXException;

import waazdoh.client.binaries.BinaryStorage;
import waazdoh.client.model.Binary;
import waazdoh.client.model.BinaryListener;
import waazdoh.client.model.MBinaryID;
import waazdoh.cp2p.P2PBinarySource;
import waazdoh.testing.StaticService;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MCRC;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;

public final class TestBinary extends WCTestCase implements BinaryStorage {
	private MLogger log = MLogger.getLogger(this);

	public void testBinary() throws IOException {

		Binary binary = new Binary(new StaticService(), this, "test", "test");
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
		Binary binary2 = new Binary(new StaticService(), this, "test2", "test");
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

		Binary b = new Binary(a.getService(), this, "test", "test");
		b.add(bs, bs.length);

		assertEquals(a.getCRC(), b.getCRC());
	}

	private String getTempPath() {
		String tempDir = System.getProperty("java.io.tmpdir");
		return tempDir;
	}

	@Override
	public String getBinaryPath(MBinaryID id) {
		return getTempPath() + File.separator + id;
	}

	public void testBinaryBean() throws IOException {
		Binary a = getNewBinary();
		byte[] bs = new byte[1000];
		a.add(bs);
		assertNotNull(a.getID());
		Binary b = new Binary(a.getService(), this, "test", "test");
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
		return new Binary(service, this, "test", "test");
	}

	public void testLoadSharedFile() throws SAXException, IOException {
		P2PBinarySource c1 = getServiceSource(getRandomUserName(), false);
		Binary b1 = c1.newBinary("test", "test");
		assertNotNull(b1);
		addData(b1);
		b1.setReady();
		b1.publish();
		//
		MPreferences p1 = c1.getPreferences();

		//
		P2PBinarySource c2 = getServiceSource(getRandomUserName(), false);
		c2.getPreferences().set(MPreferences.LOCAL_PATH,
				p1.get(MPreferences.LOCAL_PATH, "FAIL"));

		Binary b2 = c2.getOrDownload(b1.getID());
		assertNotNull(b2);
		assertTrue(b2.checkCRC());
		assertEquals(b1.getCRC(), b2.getCRC());
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
