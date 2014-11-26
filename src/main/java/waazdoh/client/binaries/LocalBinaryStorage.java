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
package waazdoh.client.binaries;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waazdoh.client.model.Binary;
import waazdoh.client.model.CMService;
import waazdoh.client.model.MBinaryID;
import waazdoh.client.model.StringIDLocalPath;
import waazdoh.util.MCRC;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public final class LocalBinaryStorage implements BinaryStorage {
	private List<Binary> streams = new LinkedList<Binary>();
	private Map<MBinaryID, MCRC> crcs = new HashMap<MBinaryID, MCRC>();
	//
	private MLogger log = MLogger.getLogger(this);
	private boolean running = true;
	private final CMService service;
	private MPreferences preferences;

	public LocalBinaryStorage(MPreferences p, CMService service) {
		this.service = service;
		this.preferences = p;
	}

	private String getLocalPath() {
		return preferences.get(MPreferences.LOCAL_PATH, ".waazdoh");
	}

	public String getMemoryUsageInfo() {
		synchronized (streams) {
			String info = "streams:" + streams.size();

			int memcount = 0;
			for (Binary b : streams) {
				memcount += b.getMemoryUsage();
			}
			info += " usage:" + memcount;
			return info;
		}
	}

	public boolean isRunning() {
		return running;
	}

	public void addNewBinary(Binary fs) {
		synchronized (streams) {
			if (findStream(fs.getID()) != null) {
				throw new RuntimeException("Binary " + fs + " already added");
			} else {
				log.info("adding binary " + fs);
				log.info("current memory usage " + getMemoryUsageInfo());
				streams.add(fs);
				streams.notifyAll();
			}
		}
	}

	public Binary getBinary(MBinaryID streamid) {
		synchronized (streams) {
			Binary fs = findStream(streamid);
			//
			if (fs == null) {
				fs = getPersistentStream(streamid);
				if (fs != null) {
					log.info("found persistent data " + streamid);
					addNewBinary(fs);
				} else {
					log.info("ERROR Stream with id " + streamid + " null");
				}
			}
			return fs;
		}
	}

	public Binary findStream(MStringID streamid) {
		synchronized (streams) {
			Binary fs = null;
			Iterator<Binary> i = new LinkedList<Binary>(streams).iterator();
			while (fs == null && i.hasNext()) {
				Binary test = i.next();
				MBinaryID testid = test.getID();
				if (testid.equals(streamid)) {
					fs = test;
				}
			}
			return fs;
		}
	}

	public synchronized Binary loadPersistentStream(MBinaryID streamid)
			throws IOException {
		synchronized (streams) {
			Binary bin;
			bin = new Binary(service, this, "default", "default");
			bin.load(streamid);

			if (bin.isOK() && bin.checkCRC()) {
				return bin;
			} else {
				return null;
			}
		}
	}

	private Binary loadPersistentBinary(Binary w) throws IOException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				getDataPath(w)));
		try {
			if (w.load(is)) {
				return w;
			} else {
				log.info("loading Binary " + w.getID() + " failed");
				return null;
			}
		} finally {
			is.close();
		}
	}

	/*
	 * public File getBinaryFile(Binary bin) { return new
	 * File(getDataPath(bin)); }
	 */

	private Binary getPersistentStream(MBinaryID streamid) {
		synchronized (streams) {
			try {
				return loadPersistentStream(streamid);
			} catch (IOException e) {
				log.error(e);
				return null;
			}
		}
	}

	private String getDataPath(Binary bin) {
		String datapath = getBinaryPath(bin.getID()) + bin.getID() + "."
				+ bin.getExtension();
		return datapath;
	}

	private String getBinaryFolderPath(MBinaryID id) {
		String binarypath = new StringIDLocalPath(getLocalPath(), id).getPath();

		//
		File file = new File(binarypath);
		file.mkdirs();
		//
		return binarypath;
	}

	public String getBinaryPath(MBinaryID id) {
		return getBinaryFolderPath(id) + File.separator + id.toString();
	}

	public File getBinaryFile(Binary b) {
		return new File(getDataPath(b));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("MBinaryStorage[");
		//
		sb.append("" + streams.size());
		//
		sb.append("]");
		return sb.toString();
	}

	public void close() {
		running = false;
		synchronized (this) {
			notifyAll();
		}
	}

	public void clearMemory(int suggestedmemorytreshold) {
		synchronized (streams) {
			List<Binary> bis = this.streams;
			List<Binary> nbis = new LinkedList<Binary>();

			for (Binary binary : bis) {
				if (binary.isUsed(suggestedmemorytreshold)) {
					nbis.add(binary);
				} else {
					log.info("not used " + binary);
				}
			}

			this.streams.clear();
			this.streams.addAll(nbis);
		}
	}

	public Binary newBinary(final String comment, String extension) {
		synchronized (streams) {
			log.info("Adding a new binary. memory usage:"
					+ getMemoryUsageInfo());
			Binary b = new Binary(service, this, comment, extension);
			addNewBinary(b);
			return b;
		}
	}

}
