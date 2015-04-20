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
package waazdoh.client.storage.local;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import waazdoh.client.WClient;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.storage.BinaryStorage;
import waazdoh.client.utils.MCRC;
import waazdoh.common.MStringID;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;

public final class LocalBinaryStorage implements BinaryStorage {
	private List<Binary> streams = new LinkedList<Binary>();
	private Map<BinaryID, MCRC> crcs = new HashMap<BinaryID, MCRC>();
	//
	private WLogger log = WLogger.getLogger(this);
	private boolean running = true;
	private final WClient client;
	private WPreferences preferences;

	public LocalBinaryStorage(WPreferences p, WClient client) {
		this.client = client;
		this.preferences = p;
	}

	private String getLocalPath() {
		return preferences.get(WPreferences.LOCAL_PATH, ".waazdoh");
	}

	public boolean isRunning() {
		return running;
	}

	public void addNewBinary(Binary fs) {
		synchronized (streams) {
			if (findBinary(fs.getID()) != null) {
				throw new RuntimeException("Binary " + fs + " already added");
			} else {
				log.info("adding binary " + fs);
				streams.add(fs);
				streams.notifyAll();
			}
		}
	}

	public Binary getBinary(BinaryID streamid) {
		synchronized (streams) {
			Binary fs = findBinary(streamid);
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

	private Binary findBinary(MStringID streamid) {
		synchronized (streams) {
			Binary fs = null;
			Iterator<Binary> i = new LinkedList<Binary>(streams).iterator();
			while (fs == null && i.hasNext()) {
				Binary test = i.next();
				BinaryID testid = test.getID();
				if (testid.equals(streamid)) {
					fs = test;
				}
			}
			return fs;
		}
	}

	public synchronized Binary loadPersistentStream(BinaryID streamid) throws IOException {
		synchronized (streams) {
			Binary bin;
			bin = new Binary(client, getLocalPath(), "default", "default");
			bin.load(streamid);

			if (bin.isOK() && bin.checkCRC()) {
				return bin;
			} else {
				return null;
			}
		}
	}

	private Binary getPersistentStream(BinaryID streamid) {
		synchronized (streams) {
			try {
				return loadPersistentStream(streamid);
			} catch (IOException e) {
				log.error(e);
				return null;
			}
		}
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
			Binary b = new Binary(client, getLocalPath(), comment, extension);
			addNewBinary(b);
			return b;
		}
	}

}
