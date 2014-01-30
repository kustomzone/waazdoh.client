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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.client.MBinaryID;
import waazdoh.client.WaazdohInfo;
import waazdoh.cp2p.impl.Download;
import waazdoh.cp2p.impl.MMessage;
import waazdoh.cp2p.impl.MNodeConnection;
import waazdoh.cp2p.impl.MNodeID;
import waazdoh.cp2p.impl.MessageResponseListener;
import waazdoh.cp2p.impl.Node;
import waazdoh.cp2p.impl.SimpleMessageHandler;
import waazdoh.cutils.MID;
import waazdoh.cutils.MLogger;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.xml.JBean;

public final class WhoHasHandler extends SimpleMessageHandler {
	private int maxResponseWaitTime = WaazdohInfo.MAX_RESPONSE_WAIT_TIME;
	//
	private MLogger log = MLogger.getLogger(this);
	/**
	 * 
	 */
	private final ByteArraySource source;
	private final MNodeConnection nodeconnection;
	//
	private Map<MID, MNodeID> whohas = new HashMap<MID, MNodeID>();
	private Map<MStringID, Integer> responsecount = new HashMap<MStringID, Integer>();

	private boolean downloadeverything;

	public WhoHasHandler(ByteArraySource source, MNodeConnection nodeconnection) {
		this.source = source;
		this.nodeconnection = nodeconnection;
	}

	@Override
	public boolean handle(final MMessage childb, final Node node) {
		final MBinaryID streamid = new MBinaryID(
				childb.getAttribute("streamid"));
		if (source.get(streamid) != null) {
			MMessage m;
			m = getFactory().newResponseMessage(childb, "stream");
			m.addIDAttribute("streamid", streamid);
			//
			JBean needed = childb.get("needed");
			List<JBean> neededpieces = needed.getChildren();
			int bytes = 0;
			while (bytes < WaazdohInfo.WHOHAS_RESPONSE_MAX_PIECE_SIZE
					&& neededpieces.size() > 0) {
				log.info("processing pieces wanted " + neededpieces);

				int pieceindex = (int) (Math.random() * neededpieces.size());
				if (pieceindex == neededpieces.size()) {
					pieceindex = neededpieces.size() - 1;
				}

				JBean neededpiece = neededpieces.remove(pieceindex);

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
				byte allbytes[] = source.get(streamid);
				byte bs[] = new byte[end - start + 1];
				bytes += bs.length;
				int index = 0;
				log.info("preparing piece " + start + " -> " + end
						+ " allbytes:" + allbytes.length + " bs:" + bs.length);
				for (int i = start; i <= end; i++) {
					bs[index++] = allbytes[i];
				}
				m.addAttachment("bytes", bs);
				m.addAttribute("start", start);
				m.addAttribute("end", end);
			}

			node.addMessage(m);
			return true;
		} else {
			if (downloadeverything) {
				source.addDownload(streamid);
			}
			//
			MNodeID knownwhohas = whohas.get(streamid);
			MessageResponseListener responselistener = new MessageResponseListener() {
				private long st = System.currentTimeMillis();

				@Override
				public void messageReceived(Node n, MMessage message) {
					log.info("whohas response " + message);

					Integer count = responsecount.get(streamid);
					if (count == null) {
						count = 0;
					}
					count++;
					responsecount.put(streamid, count);

					if (downloadeverything
							|| count > WaazdohInfo.RESPONSECOUNT_DOWNLOADTRIGGER) {
						Download download = nodeconnection
								.getDownload(streamid);
						download.messageReceived(n, message);
					}

					// node.addMessage(message);
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
				Node knownnode = nodeconnection.getNode(knownwhohas);
				knownnode.addMessage(childb, responselistener);
			}
			return true;
		}
	}

	public void downloadEveryThing(boolean b) {
		this.downloadeverything = b;
	}
}
