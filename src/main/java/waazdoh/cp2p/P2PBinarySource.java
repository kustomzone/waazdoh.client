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
import waazdoh.client.WClient;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.client.storage.local.LocalBinaryStorage;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;

public final class P2PBinarySource implements BinarySource {
	private P2PServer server;
	//
	private WLogger log = WLogger.getLogger(this);
	private WPreferences preferences;
	final private FileBeanStorage beanstorage;
	private LocalBinaryStorage storage;
	private WClient client;

	public P2PBinarySource(WPreferences p, FileBeanStorage beanstorage,
			boolean bind2) {
		this.server = new P2PServerImpl(p, bind2);
		server.setBinarySource(this);

		this.beanstorage = beanstorage;
		this.preferences = p;
	}

	public P2PBinarySource(WPreferences np, FileBeanStorage nbeanstorage,
			P2PServer nserver) {
		this.beanstorage = nbeanstorage;
		this.preferences = np;
		this.server = nserver;
		nserver.setBinarySource(this);
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
	public WClient getClient() {
		return client;
	}

	@Override
	public void setClient(WClient client) {
		this.client = client;
		storage = new LocalBinaryStorage(preferences, client);
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
		if (client != null)
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

	public void setReportingService(ReportingService reporting) {
		server.setReportingService(reporting);
	}

	@Override
	public boolean isReady() {
		return server.isConnected();
	}

	@Override
	public void waitUntilReady() {
		server.waitForConnection(preferences.getInteger(
				"waazdoh.maxtime.waituntilready", 120000));
	}

	public WPreferences getPreferences() {
		return preferences;
	}
}
