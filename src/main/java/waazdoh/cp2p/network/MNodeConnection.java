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
package waazdoh.cp2p.network;

import java.util.Set;

import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.util.MStringID;

public interface MNodeConnection {

	void addSourceListener(SourceListener sourceListener);

	boolean isRunning();

	MMessage getMessage(final String string);

	void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener);

	Node getNode(MNodeID throughtid);

	Node addNode(MHost mHost, int nport);

	void broadcastMessage(MMessage childb,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions);

	MStringID getID();

	void reportDownload(MStringID id, boolean ready);

	void removeDownload(MStringID mid);
}
