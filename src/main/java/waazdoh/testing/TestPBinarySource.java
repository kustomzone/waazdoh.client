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
package waazdoh.testing;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import waazdoh.client.binaries.BinarySource;
import waazdoh.client.binaries.LocalBinaryStorage;
import waazdoh.client.binaries.ReportingService;
import waazdoh.client.model.Binary;
import waazdoh.client.model.WService;
import waazdoh.client.model.WData;
import waazdoh.client.model.BinaryID;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public final class TestPBinarySource implements BinarySource {
	private MPreferences preferences;
	private LocalBinaryStorage storage;
	private WService service;
	private Map<String, WData> beans = new HashMap<String, WData>();

	public TestPBinarySource(MPreferences p) {
		this.preferences = p;
	}

	@Override
	public Set<MStringID> getLocalObjectIDs() {
		Set<MStringID> ret = new HashSet<MStringID>();
		Set<String> keys = beans.keySet();
		for (final String string : keys) {
			ret.add(new MStringID(string));
		}
		return ret;
	}

	public void addBinary(Binary stream) {
		storage.addNewBinary(stream);
	}

	@Override
	public File getBinaryFile(Binary b) {
		return storage.getBinaryFile(b);
	}

	@Override
	public Binary newBinary(final String string, String extension) {
		return storage.newBinary(string, extension);
	}

	@Override
	public String getMemoryUsageInfo() {
		// TODO Auto-generated method stub
		return "info";
	}

	@Override
	public void setDownloadEverything(boolean b) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setReportingService(ReportingService rservice) {
		// TODO Auto-generated method stub

	}

	@Override
	public void clearMemory(int suggestedmemorytreshold) {
		storage.clearMemory(suggestedmemorytreshold);
	}

	@Override
	public String getInfoText() {
		return "TEST";
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	@Override
	public void startClosing() {
		// TODO Auto-generated method stub

	}

	@Override
	public void addBean(final String id, WData response) {
		beans.put(id, response);
	}

	@Override
	public WData getBean(final String id) {
		return beans.get(id);
	}

	public Binary get(BinaryID fsid) {
		return storage.getBinary(fsid);
	}

	@Override
	public Binary getOrDownload(BinaryID bid) {
		Binary b = storage.getBinary(bid);
		if (b == null) {
			b = storage.newBinary("", "bin");
			b.load(bid);
			storage.addNewBinary(b);
		}
		return b;
	}

	@Override
	public WService getService() {
		return service;
	}

	@Override
	public void setService(WService service) {
		this.service = service;
		this.storage = new LocalBinaryStorage(preferences, service);
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void waitUntilReady() {
		//
	}
}
