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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.client.BinarySource;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.common.MStringID;
import waazdoh.common.ObjectID;
import waazdoh.common.WData;
import waazdoh.common.WLogger;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.messaging.SimpleMessageHandler;
import waazdoh.cp2p.network.WNode;

public final class WhoHasHandler extends SimpleMessageHandler {
	public static final String MESSAGENAME = "whohas";

	//
	private WLogger log = WLogger.getLogger(this);
	/**
	 * 
	 */
	private final BinarySource source;
	//
	private Map<ObjectID, MNodeID> whohas = new HashMap<ObjectID, MNodeID>();
	private Map<MStringID, Integer> responsecount = new HashMap<MStringID, Integer>();
	private List<WhoHasListener> listeners = new ArrayList<WhoHasListener>();

	private boolean downloadeverything;
	private P2PServer server;

	public WhoHasHandler(BinarySource source, P2PServer server, boolean downloadeverything) {
		this.source = source;
		this.server = server;
		this.downloadeverything = downloadeverything;
	}

	@Override
	public MMessage handle(final MMessage childb) {
		final BinaryID streamid = new BinaryID(childb.getAttribute("streamid"));
		Binary binary = source.get(streamid);
		if (binary != null && binary.isReady()) {
			return handleMessageWhenStreamFound(childb, streamid);
		} else {
			return handleNotFoundStream(childb, streamid);
		}
	}

	private MMessage handleNotFoundStream(final MMessage childb, final BinaryID streamid) {
		if (downloadeverything) {
			source.getOrDownload(streamid);
		}

		MNodeID knownwhohas = whohas.get(streamid);
		MessageResponseListener responselistener = new MessageResponseListener() {
			private long st = System.currentTimeMillis();

			@Override
			public void messageReceived(MMessage message) {
				log.info("whohas response " + message);

				Integer count = responsecount.get(streamid);
				if (count == null) {
					count = 0;
				}
				count++;
				responsecount.put(streamid, count);

				if (downloadeverything || count > P2PServer.RESPONSECOUNT_DOWNLOADTRIGGER) {
					fireBinaryRequested(streamid, count);
				}
			}

			@Override
			public boolean isDone() {
				return System.currentTimeMillis() - st > P2PServer.MAX_RESPONSE_WAIT_TIME;
			}
		};
		//
		if (knownwhohas == null) {
			Set<MNodeID> exceptions = new HashSet<MNodeID>();
			exceptions.add(childb.getSentBy());
			getMessenger().broadcastMessage(childb, responselistener, exceptions);
		} else {
			childb.addResponseListener(responselistener);
			WNode knownnode = server.getNode(knownwhohas);
			knownnode.sendMessage(childb);
		}
		//
		return null;
	}

	private MMessage handleMessageWhenStreamFound(final MMessage childb, final BinaryID streamid) {
		MMessage m;
		m = getMessenger().newResponseMessage(childb, "stream");
		m.addIDAttribute("streamid", streamid);
		//
		WData needed = childb.get("needed");
		List<WData> neededpieces = needed.getChildren();
		int bytes = 0;
		while (bytes < P2PServer.WHOHAS_RESPONSE_MAX_PIECE_SIZE && !neededpieces.isEmpty()) {
			log.info("processing pieces wanted " + neededpieces);

			int pieceindex = (int) (Math.random() * neededpieces.size());
			if (pieceindex == neededpieces.size()) {
				pieceindex = neededpieces.size() - 1;
			}

			WData neededpiece = neededpieces.remove(pieceindex);

			int start = neededpiece.getIntValue("start");
			int end = neededpiece.getIntValue("end");
			if (end - start > P2PServer.WHOHAS_RESPONSE_MAX_PIECE_SIZE) {
				start += (int) ((end - start) * Math.random());
				while (start + P2PServer.WHOHAS_RESPONSE_MAX_PIECE_SIZE > end) {
					start--;
				}
				end = start + P2PServer.WHOHAS_RESPONSE_MAX_PIECE_SIZE;
			}
			//
			Binary bin = source.getOrDownload(streamid);
			byte bs[] = new byte[end - start + 1];
			bytes += bs.length;

			log.info("preparing piece " + start + " -> " + end + " bin:" + bin + " bs:" + bs.length);
			try {
				bin.read(start, bs);
				m.addAttachment("bytes", bs);
				m.addAttribute("start", start);
				m.addAttribute("end", end);
			} catch (IOException e) {
				log.error(e);
			}
		}

		return m;
	}

	public void downloadEveryThing(boolean b) {
		this.downloadeverything = b;
	}

	private void fireBinaryRequested(BinaryID streamid, Integer count) {
		for (WhoHasListener whoHasListener : listeners) {
			whoHasListener.binaryRequested(streamid, count);
		}
	}

	public void addListener(WhoHasListener listener) {
		this.listeners.add(listener);
	}
}
