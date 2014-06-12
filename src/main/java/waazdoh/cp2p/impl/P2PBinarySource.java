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
package waazdoh.cp2p.impl;

import java.io.File;
import java.util.Set;

import waazdoh.client.Binary;
import waazdoh.client.BinaryListener;
import waazdoh.client.MBinaryID;
import waazdoh.client.MBinarySource;
import waazdoh.client.MBinaryStorage;
import waazdoh.cp2p.impl.handlers.ByteArraySource;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.xml.JBean;
import waazdoh.service.CMService;
import waazdoh.service.ReportingService;

public final class P2PBinarySource implements MBinarySource {
	P2PServer server;
	//
	MLogger log = MLogger.getLogger(this);
	private MPreferences preferences;
	private MBeanStorage beanstorage;
	private ReportingService reporting;
	private MBinaryStorage storage;
	private CMService service;

	public P2PBinarySource(MPreferences p, Object reportingservice) {
		this(p, true);
	}

	public P2PBinarySource(MPreferences p, boolean bind2) {
		this.server = new P2PServer(p, bind2, new ByteArraySource() {
			@Override
			public byte[] get(MBinaryID streamid) {
				Binary fs = P2PBinarySource.this.get(streamid);
				if (fs == null || !fs.isReady()) {
					return null;
				} else {
					return fs.asByteBuffer();
				}
			}

			@Override
			public void addDownload(MBinaryID streamid) {
				getOrDownload(streamid);
			}
		});

		this.preferences = p;
	}

	@Override
	public File getBinaryFile(Binary bin) {
		return this.storage.getBinaryFile(bin);
	}

	@Override
	public String getMemoryUsageInfo() {
		String info = "";
		info += "storage:[" + storage.getMemoryUsageInfo() + "]";
		info += "server:[" + server.getMemoryUserInfo() + "]";
		return info;
	}

	@Override
	public boolean reload(Binary binary) {
		return this.storage.reload(binary) != null;
	}

	@Override
	public String getInfoText() {
		String info = "server:" + server.getInfoText();
		return info;
	}

	@Override
	public String toString() {
		return "P2PBinarySource[" + server + "]";
	}

	@Override
	public boolean isRunning() {
		return storage != null && this.beanstorage != null
				&& server.isRunning();
	}

	@Override
	public CMService getService() {
		return service;
	}

	@Override
	public synchronized void addBean(final String id, JBean response) {
		beanstorage.addBean(id, response);
	}

	@Override
	public void setService(CMService service) {
		this.service = service;
		storage = new MBinaryStorage(preferences, service);
		beanstorage = new MBeanStorage(preferences);
		server.start();
	}

	@Override
	public JBean getBean(final String id) {
		return beanstorage.getBean(id);
	}

	private synchronized Binary get(MBinaryID streamid) {
		Binary fs = storage.getBinary(streamid);
		return fs;
	}

	private void clearFromMemory(int time, MID binaryid) {
		log.info("clear from memory " + binaryid + " time:" + time);
		storage.clearFromMemory(time, binaryid);
	}

	@Override
	public void clearMemory(int suggestedmemorytreshold) {
		if (storage != null)
			storage.clearMemory(suggestedmemorytreshold);
		if (service != null)
			server.clearMemory(suggestedmemorytreshold);
	}

	@Override
	public Set<MStringID> getLocalObjectIDs() {
		return beanstorage.getLocalSetIDs();
	}

	@Override
	public synchronized Binary getOrDownload(MBinaryID fsid) {
		Binary fs = get(fsid);
		if (fs == null) {
			if (server.waitForDownloadSlot(5000)) {
				log.info("new Binary " + fsid);
				fs = new Binary(fsid, service);
				if (fs.isOK()) {
					addBinary(fs);
					addDownload(fs);
				} else {
					fs = null;
				}
			} else {
				log.info("Cannot download " + fsid
						+ ". Download queue is full ");

			}
			//
		}
		return fs;
	}

	private synchronized void addDownload(final Binary bin) {
		bin.addListener(new BinaryListener() {
			@Override
			public void ready(Binary binary) {
				saveBinaries();
			}
		});
		server.addDownload(bin);
	}

	@Override
	public Binary newBinary(final String string, String extension) {
		return storage.newBinary(string, extension);
	}

	private synchronized void addBinary(Binary stream) {
		storage.addNewBinary(stream);
	}

	@Override
	public void saveBinaries() {
		storage.saveBinaries();
	}

	@Override
	public void close() {
		if (storage != null) {
			storage.close();
		}
		server.close();
	}

	public void setDownloadEverything(boolean b) {
		server.setDownloadEverything(b);
	}

	public void setReportingService(ReportingService reporting) {
		server.setReportingService(reporting);
	}

	@Override
	public boolean isReady() {
		return server.isConnected();
	}

	@Override
	public void waitUntilReady() {
		try {
			server.waitForConnection();
		} catch (InterruptedException e) {
			log.error(e);
		}
	}
}
