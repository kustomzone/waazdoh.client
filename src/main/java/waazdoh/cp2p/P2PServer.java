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
import waazdoh.common.MStringID;
import waazdoh.common.WPreferences;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.network.ServerListener;
import waazdoh.cp2p.network.WNode;

public interface P2PServer {
	String DOWNLOAD_EVERYTHING = "network.download.everything";

	// should be larger than WHOHAS_RESPONSE_MAX_PIECE_SIZE
	public static final int DOWNLOADER_MAX_REQUESTED_PIECELENGTH = 200000;

	public static final int WHOHAS_RESPONSE_MAX_PIECE_SIZE = 100000;
	public static final Integer RESPONSECOUNT_DOWNLOADTRIGGER = 20;
	public static final int MAX_RESPONSE_WAIT_TIME = 40000;
	public static final int DOWNLOAD_RESET_DELAY = 8000;

	boolean isRunning();

	void close();

	WNode getNode(MNodeID id);

	Iterable<WNode> getNodesIterator();

	WNode addNode(MNodeID sentby);

	WNode addNode(MHost mHost, int i);

	void clearMemory();

	boolean canDownload();

	WPreferences getPreferences();

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
