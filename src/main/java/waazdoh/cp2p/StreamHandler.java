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

import waazdoh.common.MStringID;
import waazdoh.common.WLogger;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.cp2p.network.WMessenger;

public final class StreamHandler implements MMessageHandler {
	private P2PServer source;
	private WLogger log = WLogger.getLogger(this);
	private WMessenger messenger;

	public StreamHandler(P2PServer server) {
		this.source = server;
	}

	@Override
	public MMessage handle(MMessage childb) {
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
				download.messageReceived(childb);
				return null;
			} else {
				log.error("download not found " + streamid);
				return null;
			}
		} else {
			log.error("StreamID null");
			log.info("streamid null " + childb);
			return null;
		}
	}

	@Override
	public void setMessenger(final WMessenger nmessenger) {
		this.messenger = nmessenger;
	}
}
