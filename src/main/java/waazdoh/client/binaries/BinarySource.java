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

import java.io.File;
import java.util.Set;

import waazdoh.client.model.Binary;
import waazdoh.client.model.CMService;
import waazdoh.client.model.JBean;
import waazdoh.client.model.MBinaryID;
import waazdoh.util.MStringID;

public interface BinarySource {
	void close();

	boolean isRunning();

	void setService(CMService service);

	CMService getService();

	Binary get(MBinaryID streamid);

	Binary getOrDownload(MBinaryID binaryid);

	void clearMemory(int suggestedmemorytreshold);

	JBean getBean(final String string);

	void addBean(final String string, JBean response);

	String getInfoText();

	String getMemoryUsageInfo();

	void setDownloadEverything(boolean b);

	void setReportingService(ReportingService rservice);

	Binary newBinary(final String comment, String extension);

	File getBinaryFile(Binary bin);

	Set<MStringID> getLocalObjectIDs();

	void waitUntilReady();

	boolean isReady();

	void startClosing();

}