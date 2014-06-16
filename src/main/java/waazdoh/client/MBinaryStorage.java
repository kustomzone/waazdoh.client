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
package waazdoh.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waazdoh.cutils.MCRC;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.service.CMService;

public final class MBinaryStorage {
	private List<Binary> streams = new LinkedList<Binary>();
	private Map<MBinaryID, MCRC> crcs = new HashMap<MBinaryID, MCRC>();
	//
	private MLogger log = MLogger.getLogger(this);
	private final String localpath;
	private boolean running = true;
	private final CMService service;

	public MBinaryStorage(MPreferences p, CMService service) {
		this.localpath = p.get(MPreferences.LOCAL_PATH, ".waazdoh");
		this.service = service;
		//
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				while (isRunning()) {
					try {
						saveBinaries();
						synchronized (streams) {
							streams.wait(6000);
						}
					} catch (InterruptedException e) {
						log.error(e);
					} catch (Exception e) {
						log.error(e);
					}
				}
			}
		}, "Storage save binaries");
		t.start();
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
					streams.add(fs);
				} else {
					log.info("ERROR Stream with id " + streamid + " null");
				}
			}
			return fs;
		}
	}

	public void clearFromMemory(int time, MID binaryid) {
		saveBinaries();
		Binary persistentStream = findStream(binaryid.getStringID());
		log.info("clear from memory " + persistentStream + " time:" + time);
		if (persistentStream != null) {
			if (!persistentStream.isUsed(time)) {
				log.info("removing " + binaryid);
				synchronized (streams) {
					streams.remove(findStream(binaryid.getStringID()));
				}
			}
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

	public void saveBinaries() {
		synchronized (streams) {
			Collection<Binary> lbinaries = streams;
			for (Binary bin : lbinaries) {
				try {
					saveBinary(bin);
				} catch (IOException e) {
					log.error(e);
				}
			}
		}
	}

	private void saveBinary(Binary fs) throws FileNotFoundException {
		synchronized (streams) {
			MCRC persistentBinaryCRC = getPersistentBinaryTimestamp(fs.getID());
			MCRC fscrc = fs.getCRC();
			if ((persistentBinaryCRC == null || !persistentBinaryCRC
					.equals(fscrc)) && fs.isReady()) {
				String datapath = getDataPath(fs);
				log.info("saving binary datapath:" + datapath);
				fs.save(new BufferedOutputStream(new FileOutputStream(datapath)));
				crcs.put(fs.getID(), fs.getCRC());
			}
		}
	}

	public Binary reload(Binary binary) {
		try {
			return loadPersistentBinary(binary);
		} catch (FileNotFoundException e) {
			log.error(e);
			return null;
		}
	}

	public synchronized Binary loadPersistentStream(MBinaryID streamid)
			throws FileNotFoundException {
		synchronized (streams) {
			Binary bin;
			bin = new Binary(streamid, service);
			if (bin.isOK()) {
				String datapath = getDataPath(bin);
				log.info("loading persistent binary " + streamid + " datapath:"
						+ datapath);
				File f = new File(datapath);
				if (f.exists()) {
					return loadPersistentBinary(bin);
				} else {
					return null;
				}
			} else {
				return null;
			}
		}
	}

	private Binary loadPersistentBinary(Binary w) throws FileNotFoundException {
		BufferedInputStream is = new BufferedInputStream(new FileInputStream(
				getDataPath(w)));
		if (w.load(is)) {
			return w;
		} else {
			log.info("loading Binary " + w.getID() + " failed");
			return null;
		}
	}

	public File getBinaryFile(Binary bin) {
		return new File(getDataPath(bin));
	}

	private MCRC getPersistentBinaryTimestamp(MBinaryID mBinaryID) {
		return crcs.get(mBinaryID);
	}

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

	private String getBinaryPath(MBinaryID mBinaryID) {
		String sid = mBinaryID.toString();
		String binarypath = new StringIDLocalPath(this.localpath, mBinaryID)
				.getPath();

		//
		File file = new File(binarypath);
		file.mkdirs();
		//
		return binarypath;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("MBinaryStorage[");
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
			saveBinaries();

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
			Binary b = new Binary(service, comment, extension);
			this.streams.add(b);
			return b;
		}
	}

}
