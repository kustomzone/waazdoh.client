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

import waazdoh.client.BinarySource;
import waazdoh.client.ReportingService;
import waazdoh.client.WClient;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.storage.local.LocalBinaryStorage;
import waazdoh.common.WPreferences;

public final class TestPBinarySource implements BinarySource {
	private WPreferences preferences;
	private LocalBinaryStorage storage;
	private WClient service;

	public TestPBinarySource(WPreferences p) {
		this.preferences = p;
	}

	public void addBinary(Binary stream) {
		storage.addNewBinary(stream);
	}

	@Override
	public Binary newBinary(final String string, String extension) {
		return storage.newBinary(string, extension);
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
	public WClient getClient() {
		return service;
	}

	@Override
	public void setClient(WClient nclient) {
		this.service = nclient;
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
