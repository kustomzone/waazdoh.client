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
package waazdoh.cp2p;

import waazdoh.client.BinarySource;
import waazdoh.client.ReportingService;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.service.WService;
import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.client.storage.local.LocalBinaryStorage;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;

public final class P2PBinarySource implements BinarySource {
	P2PServer server;
	//
	MLogger log = MLogger.getLogger(this);
	private MPreferences preferences;
	final private FileBeanStorage beanstorage;
	private LocalBinaryStorage storage;
	private WService service;

	public P2PBinarySource(MPreferences p, FileBeanStorage beanstorage,
			boolean bind2) {
		this.server = new P2PServer(p, bind2, this);
		this.beanstorage = beanstorage;
		this.preferences = p;
	}

	@Override
	public String getMemoryUsageInfo() {
		String info = "";
		info += "storage:[" + storage.getMemoryUsageInfo() + "]";
		info += "server:[" + server.getMemoryUserInfo() + "]";
		return info;
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
	public WService getService() {
		return service;
	}

	@Override
	public void setService(WService service) {
		this.service = service;
		storage = new LocalBinaryStorage(preferences, service);
		server.start();
	}

	public synchronized Binary get(BinaryID streamid) {
		Binary fs = storage.getBinary(streamid);
		return fs;
	}

	@Override
	public void clearMemory(int suggestedmemorytreshold) {
		if (storage != null)
			storage.clearMemory(suggestedmemorytreshold);
		if (service != null)
			server.clearMemory();
	}

	@Override
	public synchronized Binary getOrDownload(BinaryID fsid) {
		Binary fs = get(fsid);
		if (fs == null) {
			if (server.waitForDownloadSlot(5000)) {
				log.info("new Binary " + fsid);

				fs = storage.newBinary("", "");

				if (fs.load(fsid) && fs.isOK()) {
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
	public void close() {
		if (storage != null) {
			storage.close();
		}
		server.close();
	}

	@Override
	public void startClosing() {
		server.startClosing();
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
			server.waitForConnection(preferences.getInteger(
					"waazdoh.maxtime.waituntilready", 120000));
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	public MPreferences getPreferences() {
		return preferences;
	}
}
