/*******************************************************************************
 * Copyright (c) 2013 Juuso Vilmunen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Juuso Vilmunen - initial API and implementation
 ******************************************************************************/
package waazdoh.client.model.objects;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.LinkedList;
import java.util.List;

import waazdoh.client.model.BinaryID;
import waazdoh.client.model.StringIDLocalPath;
import waazdoh.client.model.UserID;
import waazdoh.client.model.WData;
import waazdoh.client.model.WaazdohInfo;
import waazdoh.client.service.WService;
import waazdoh.util.HashSource;
import waazdoh.util.MCRC;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;

public final class Binary implements HashSource {
	private static final String BEAN_TAG = "binary";
	//
	private long length = -1;
	private MCRC crc;
	private long timestamp;
	//
	private MLogger log = MLogger.getLogger(this);
	private MCRC storedcrc;
	private List<BinaryListener> listeners;
	//
	private UserID creatorid;
	private String version;
	private String comment = "";
	private long usedtime;
	private boolean ready;
	private WService service;
	private String extension;
	private BinaryID id;

	private RandomAccessFile access;
	private String storage;

	private static int count = 0;

	public Binary(WService service, String storagepath, String comment,
			String extension) {
		this.service = service;
		this.storage = storagepath;

		this.id = new BinaryID();
		creatorid = service.getUserID();
		//
		if (service != null) {
			this.creatorid = service.getUserID();
		}
		version = WaazdohInfo.version;
		this.extension = extension;
		this.comment = comment;

		count++;

		used();
	}

	public boolean load(BinaryID streamid) {
		this.id = streamid;
		return loadFromService(streamid);
	}

	@Override
	public String getHash() {
		return getBean().getContentHash();
	}

	public int getMemoryUsage() {
		return 0;
	}

	private void init() {
		resetCRC();
	}

	public synchronized boolean load(InputStream is) {
		try {
			newFile();

			log.info("" + this + " loading from inputstream " + is);

			byte[] nbytes = new byte[1000];
			// @
			while (true) {
				int count = is.read(nbytes);
				if (count <= 0) {
					break;
				}
				add(nbytes, count);
			}
			crc = null;
			return true;
		} catch (IOException e) {
			log.error(e);
			return false;
		}
	}

	private synchronized void newFile() throws IOException {
		log.info("new file");
		RandomAccessFile a = getAccessFile();
		a.seek(0);
		a.setLength(0);
	}

	private RandomAccessFile getAccessFile() throws IOException {
		if (access == null) {
			String filepath = getBinaryPath();
			log.info("accessing file " + filepath);
			access = new RandomAccessFile(new File(filepath), "rw");
		}
		return access;
	}

	private String getBinaryPath() {
		StringIDLocalPath p = new StringIDLocalPath(storage, id);

		File f = new File(p.getPath());
		if (!f.isDirectory()) {
			f.mkdirs();
		}

		return p.getPath() + File.separator + id + "." + extension;
	}

	public synchronized void addAt(int index, byte[] nbytes) throws IOException {
		RandomAccessFile file = getAccessFile();
		file.seek(index);
		file.write(nbytes, 0, nbytes.length);
		//
		log.debug("added " + nbytes.length + " at " + index
				+ ". File size now " + file.length());
		resetCRC();
	}

	public void addAt(int index, byte[] nbytes, int length) throws IOException {
		RandomAccessFile file = getAccessFile();
		file.seek(index);
		file.write(nbytes, 0, length);
		//
		log.debug("added " + length + " at " + index + ". File size now "
				+ file.length());
		resetCRC();
	}

	public synchronized void add(byte[] nbytes, int length) throws IOException {
		used();
		RandomAccessFile f = getAccessFile();
		f.write(nbytes, 0, length);

		log.debug("added " + nbytes.length + ". File size now " + f.length());

		resetCRC();
	}

	public synchronized void add(byte[] bytes) throws FileNotFoundException,
			IOException {
		used();
		getAccessFile().write(bytes);
		log.debug("added " + bytes.length + ". File size now "
				+ getAccessFile().length());

		resetCRC();
	}

	public synchronized void add(Byte b) throws IOException {
		getAccessFile().write(b.intValue());

		resetCRC();
	}

	public synchronized void read(int start, byte[] bs) throws IOException {
		if (isReady()) {
			RandomAccessFile f = getAccessFile();
			f.seek(start);
			f.read(bs);
			closeFile();
		} else {
			throw new RuntimeException("Binary is not ready to be read");
		}
	}

	// public FloatStream(JBean b, CMService service) {
	// this.service = service;
	// load(b);
	// }
	private void load(WData b) {
		if (b.get(BEAN_TAG) != null) {
			b = b.get(BEAN_TAG);
			id = new BinaryID(b.getAttribute("id"));
		}
		//
		this.length = b.getIntValue("length");
		this.storedcrc = new MCRC(b.getLongValue("crc"));
		this.creatorid = new UserID(b.getValue("creator"));
		this.version = b.getValue("version");
		this.extension = b.getValue("extension");
		this.comment = b.getValue("comment");
	}

	private synchronized boolean loadFromService(MStringID pid) {
		WData b = service.read(pid);
		if (b != null && (b.get("data") != null || b.get("binary") != null)) {
			log.info("loading Binary " + b);
			load(b.find(BEAN_TAG));
			used();
			return true;
		} else {
			log.info("Service read " + pid + " failed");
			return false;
		}
	}

	public void publish() {
		save();
		service.publish(getID());
	}

	public void save() {
		creatorid = service.getUserID();

		WData bean = getBean();
		bean.setAttribute("id", getID().toString());
		//
		service.addBean(getID(), bean);
	}

	@Override
	public String toString() {
		return "Binary[" + super.toString() + ":" + getID() + "][no:" + count
				+ "][" + length() + "][scrc:" + storedcrc + "][crc:" + crc
				+ "][" + comment + "]";
	}

	public boolean isOK() {
		if (id == null || storedcrc == null || creatorid == null) {
			return false;
		}
		return true;
	}

	public WData getBean() {
		WData b = new WData(BEAN_TAG);
		//
		b.addValue("length", "" + length);
		b.addValue("crc", "" + currentCRC().getValue());
		b.addValue("creator", "" + creatorid);
		b.addValue("version", version);
		b.addValue("comment", "" + comment);
		b.addValue("extension", extension);
		return b;
	}

	/*
	 * public synchronized int read(int index, byte bs[]) throws IOException {
	 * used(); RandomAccessFile f = getFile(); return f.read(bs, index,
	 * bs.length); }
	 */

	public long length() {
		return length;
	}

	private void used() {
		this.usedtime = System.currentTimeMillis();
	}

	synchronized MCRC currentCRC() {
		try {
			String binaryPath = getBinaryPath();
			if (new File(binaryPath).exists()) {
				MCRC ncrc = new MCRC(getInputStream());
				return ncrc;
			} else {
				log.info("currentCRC file not found " + binaryPath);
				return new MCRC();
			}
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	public InputStream getInputStream() throws FileNotFoundException {
		return new BufferedInputStream(new FileInputStream(getBinaryPath()));
	}

	public BinaryID getID() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public synchronized MCRC getCRC() {
		if (crc == null) {
			crc = currentCRC();
		}
		return crc;
	}

	public synchronized void resetCRC() {
		if (crc != null) {
			crc = null;
		}
		timestamp = System.currentTimeMillis();
	}

	public synchronized boolean isReady() {
		boolean currentlyready = getCRC().equals(storedcrc);
		if (currentlyready) {
			if (!ready) {
				ready = currentlyready;
				log.info("" + this + " READY with " + getCRC());
				fireReady();
			}
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Binary) {
			Binary bin = (Binary) obj;
			if (!bin.getID().equals(getID()))
				return false;
			if (!getCRC().equals(bin.getCRC()))
				return false;
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	public void setReady() {
		log.info("setting binary ready");

		used();

		try {
			length = getAccessFile().length();
			closeFile();
			storedcrc = currentCRC();
			fireReady();
		} catch (IOException e) {
			log.error(e);
		}
	}

	private void closeFile() throws IOException {
		if (access != null) {
			log.info("closing file " + access);
			this.access.close();
			this.access = null;
		}
	}

	private void fireReady() {
		if (listeners != null) {
			final Binary me = this;
			List<BinaryListener> ls = this.listeners;
			for (final BinaryListener listener : ls) {
				log.info("notifying READY " + listener);
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						listener.ready(me);
					}
				}, "Binary FireReady");
				t.start();
			}
		}
	}

	public void addListener(BinaryListener listener) {
		getListeners().add(listener);
	}

	private List<BinaryListener> getListeners() {
		if (listeners == null) {
			listeners = new LinkedList<BinaryListener>();
		}
		return listeners;
	}

	public void clear() {
		init();
	}

	public boolean isUsed(int suggestedmemorytreshold) {
		if (isReady()) {
			long dtime = System.currentTimeMillis() - usedtime;
			boolean ret = dtime < suggestedmemorytreshold;
			log.info("isused " + ret + " " + dtime);
			return ret;
		} else {
			return true;
		}
	}

	public String getExtension() {
		return extension;
	}

	public synchronized void importStream(InputStream stream)
			throws IOException {
		BufferedInputStream bis = new BufferedInputStream(stream);
		byte bs[] = new byte[1024];
		while (true) {
			int read = bis.read(bs);
			if (read < 0) {
				break;
			}
			add(bs, read);
		}
		bis.close();
		setReady();
	}

	public WService getService() {
		return service;
	}

	public synchronized boolean checkCRC() {
		return getCRC().equals(this.storedcrc);
	}

	public synchronized void flush() {
		try {
			closeFile();
			resetCRC();
		} catch (IOException e) {
			log.error(e);
		}
	}

	public File getFile() {
		return new File(getBinaryPath());
	}
}