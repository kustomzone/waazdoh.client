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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.client.BinarySource;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.ObjectID;
import waazdoh.client.model.WData;
import waazdoh.client.model.WaazdohInfo;
import waazdoh.client.model.objects.Binary;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.messaging.SimpleMessageHandler;
import waazdoh.cp2p.network.MNodeConnection;
import waazdoh.cp2p.network.Node;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;

public final class WhoHasHandler extends SimpleMessageHandler {
	private int maxResponseWaitTime = WaazdohInfo.MAX_RESPONSE_WAIT_TIME;
	//
	private MLogger log = MLogger.getLogger(this);
	/**
	 * 
	 */
	private final BinarySource source;
	private final MNodeConnection nodeconnection;
	//
	private Map<ObjectID, MNodeID> whohas = new HashMap<ObjectID, MNodeID>();
	private Map<MStringID, Integer> responsecount = new HashMap<MStringID, Integer>();
	private List<WhoHasListener> listeners = new LinkedList<WhoHasListener>();

	private boolean downloadeverything;

	public WhoHasHandler(BinarySource source, MNodeConnection nodeconnection) {
		this.source = source;
		this.nodeconnection = nodeconnection;
	}

	@Override
	public MMessage handle(final MMessage childb) {
		final BinaryID streamid = new BinaryID(
				childb.getAttribute("streamid"));
		if (source.get(streamid) != null) {
			MMessage m;
			m = getFactory().newResponseMessage(childb, "stream");
			m.addIDAttribute("streamid", streamid);
			//
			WData needed = childb.get("needed");
			List<WData> neededpieces = needed.getChildren();
			int bytes = 0;
			while (bytes < WaazdohInfo.WHOHAS_RESPONSE_MAX_PIECE_SIZE
					&& !neededpieces.isEmpty()) {
				log.info("processing pieces wanted " + neededpieces);

				int pieceindex = (int) (Math.random() * neededpieces.size());
				if (pieceindex == neededpieces.size()) {
					pieceindex = neededpieces.size() - 1;
				}

				WData neededpiece = neededpieces.remove(pieceindex);

				int start = neededpiece.getIntValue("start");
				int end = neededpiece.getIntValue("end");
				if (end - start > WaazdohInfo.WHOHAS_RESPONSE_MAX_PIECE_SIZE) {
					start += (int) ((end - start) * Math.random());
					while (start + WaazdohInfo.WHOHAS_RESPONSE_MAX_PIECE_SIZE > end) {
						start--;
					}
					end = start + WaazdohInfo.WHOHAS_RESPONSE_MAX_PIECE_SIZE;
				}
				//
				Binary bin = source.getOrDownload(streamid);
				byte bs[] = new byte[end - start + 1];
				bytes += bs.length;
				int index = 0;
				log.info("preparing piece " + start + " -> " + end + " bin:"
						+ bin + " bs:" + bs.length);
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
		} else {
			if (downloadeverything) {
				source.getOrDownload(streamid);
			}
			//
			final List<WhoHasListener> listeners = this.listeners;

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

					if (downloadeverything
							|| count > WaazdohInfo.RESPONSECOUNT_DOWNLOADTRIGGER) {
						fireBinaryRequested(streamid, count);
					}
				}

				@Override
				public boolean isDone() {
					return System.currentTimeMillis() - st > maxResponseWaitTime;
				}
			};
			//
			if (knownwhohas == null) {
				Set<MNodeID> exceptions = new HashSet<MNodeID>();
				exceptions.add(childb.getSentBy());
				nodeconnection.broadcastMessage(childb, responselistener,
						exceptions);
			} else {
				childb.addResponseListener(responselistener);
				Node knownnode = nodeconnection.getNode(knownwhohas);
				knownnode.addMessage(childb);
			}
			//
			return null;
		}
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
