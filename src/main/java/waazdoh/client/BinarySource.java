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

import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.service.WService;

public interface BinarySource {
	void close();

	boolean isRunning();

	void setService(WService service);

	WService getService();

	Binary get(BinaryID streamid);

	Binary getOrDownload(BinaryID binaryid);

	void clearMemory(int suggestedmemorytreshold);

	String getInfoText();

	String getMemoryUsageInfo();

	void setDownloadEverything(boolean b);

	void setReportingService(ReportingService rservice);

	Binary newBinary(final String comment, String extension);

	void waitUntilReady();

	boolean isReady();

	void startClosing();

}
