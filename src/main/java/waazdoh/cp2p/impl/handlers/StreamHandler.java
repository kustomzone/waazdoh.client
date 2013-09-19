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
package waazdoh.cp2p.impl.handlers;

import waazdoh.client.MStringID;
import waazdoh.cp2p.impl.Download;
import waazdoh.cp2p.impl.MMessage;
import waazdoh.cp2p.impl.MMessageFactory;
import waazdoh.cp2p.impl.MMessageHandler;
import waazdoh.cp2p.impl.Node;
import waazdoh.cp2p.impl.P2PServer;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;

public final class StreamHandler implements MMessageHandler {
	private MMessageFactory factory;
	private P2PServer source;
	private MLogger log = MLogger.getLogger(this);

	public StreamHandler(P2PServer server) {
		this.source = server;
	}

	@Override
	public boolean handle(MMessage childb, Node node) {
		/**
		 * MMessage:<stream>
		 * <streamid>396fe581-0f22-4260-ae6e-ba41a8e73024.a0ca8b91
		 * -8bb5-419a-9659-df6a4f99716b</streamid> <start>17594</start>
		 * <sentby>ad92268f
		 * -57f5-43a3-802b-55d994c061f2.9808a6f7-41ca-4c9f-b615-c0b7f623925d
		 * </sentby>
		 * <nodeid>ad92268f-57f5-43a3-802b-55d994c061f2.9808a6f7-41ca-4
		 * c9f-b615-c0b7f623925d</nodeid>
		 * <messageid>83bacc47-a967-4d83-824b-a271e80e916f
		 * .3fe1343a-2afb-4b08-b4ba-ea14315ac8f0</messageid> <end>19594</end>
		 * <date>Thu Feb 16 23:00:04 EET 2012</date> </stream>
		 */
		MStringID streamid = childb.getIDAttribute("streamid");
		if (streamid != null) {
			Download download = source.getDownload(streamid);
			if (download != null) {
				download.messageReceived(source.getNode(childb.getSentBy()),
						childb);
				return true;
			} else {
				log.error("download not found " + streamid);
				return false;
			}
		} else {
			log.error("StreamID null");
			log.info("streamid null " + childb);
			return false;
		}
	}

	@Override
	public void setFactory(MMessageFactory factory) {
		this.factory = factory;
	}
}
