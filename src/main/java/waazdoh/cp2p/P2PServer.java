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
import waazdoh.client.model.objects.Binary;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.network.ServerListener;
import waazdoh.cp2p.network.WMessenger;
import waazdoh.cp2p.network.WNode;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public interface P2PServer {
	String DOWNLOAD_EVERYTHING = "network.download.everything";

	boolean isRunning();

	void close();

	WNode getNode(MNodeID id);

	Iterable<WNode> getNodesIterator();

	WNode addNode(MNodeID sentby);

	WNode addNode(MHost mHost, int i);

	void clearMemory();

	boolean canDownload();

	MPreferences getPreferences();

	void addDownload(Binary b);

	void removeDownload(MStringID downloadid);

	boolean isConnected();

	WMessenger getMessenger();

	void reportDownload(MStringID mStringID, boolean b);

	MNodeID getID();

	Download getDownload(MStringID id);

	NodeStatus getNodeStatus(WNode n);

	void addServerListener(ServerListener sourceListener);

	int getPort();

	void start();

	void setReportingService(ReportingService reportingService);

	boolean waitForDownloadSlot(int i);

	void startClosing();

	void waitForConnection(int integer);

	String getInfoText();

	void setBinarySource(BinarySource storage);

}
