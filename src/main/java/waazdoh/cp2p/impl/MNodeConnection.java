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
package waazdoh.cp2p.impl;

import java.util.Set;

import waazdoh.cutils.MID;

public interface MNodeConnection {

	void removeDownload(MID id);

	void addSourceListener(SourceListener sourceListener);

	boolean isRunning();

	MMessage getMessage(String string);

	void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener);

	Node getNode(MID throughtid);

	void broadcastMessage(MMessage childb,
			MessageResponseListener messageResponseListener, Set<MID> exceptions);

	MID getID();

	Download getDownload(MID streamid);

	void reportDownload(MID id, boolean ready);
}
