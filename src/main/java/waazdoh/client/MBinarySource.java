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

import java.io.File;
import java.util.Set;

import waazdoh.cutils.JBeanResponse;
import waazdoh.cutils.MStringID;
import waazdoh.service.CMService;
import waazdoh.service.ReportingService;

public interface MBinarySource {
	void saveWaves();

	void close();

	boolean isRunning();

	void setService(CMService service);

	CMService getService();

	Binary getOrDownload(MBinaryID binaryid);

	void clearMemory(int suggestedmemorytreshold);

	JBeanResponse getBean(String string);

	void addBean(String string, JBeanResponse response);

	String getInfoText();

	String getMemoryUsageInfo();

	void setDownloadEverything(boolean b);

	void setReportingService(ReportingService rservice);

	Binary newBinary(String string, String extension);

	File getBinaryFile(Binary bin);

	boolean reload(Binary binary);

	Set<MStringID> getLocalObjectIDs();

}
