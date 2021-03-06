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

import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.common.MStringID;
import waazdoh.common.MTimedFlag;
import waazdoh.common.WData;
import waazdoh.common.WLogger;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.ServerListener;
import waazdoh.cp2p.network.WNode;

public final class Download implements Runnable, MessageResponseListener,
		ServerListener {
	private static final String MESSAGENAME_WHOHAS = "whohas";
	private static final String MESSAGENAME_STREAM = "stream";
	private static final int GIVEUP_TIMEOUT_MSEC = 1000 * 60 * 4;
	private static final int PART_SIZE = 100000;
	private Binary bin;
	private WMessenger messenger;
	private WLogger log;
	private MTimedFlag flag;
	private MTimedFlag giveupflag = new MTimedFlag(GIVEUP_TIMEOUT_MSEC);
	private long endtime;
	private long starttime;
	private Map<Integer, DownloadPart> sentstarts = new HashMap<Integer, DownloadPart>();
	private List<DownloadPart> missingparts = new LinkedList<DownloadPart>();

	private boolean hasBeenReady;
	private String speedinfo;

	private int countbytes;
	private P2PServer server;

	public Download(Binary b, P2PServer server, WMessenger nmessenger) {
		this.bin = b;
		this.messenger = nmessenger;
		this.server = server;
		//
		server.addServerListener(this);

		log = WLogger.getLogger(this);
		log.info("New Download starting " + b);
		//
		initMissingParts();

		Thread t = new Thread(this, "Download");
		t.start();

		log.info("created");
	}

	private void initMissingParts() {
		for (int i = 0; i < bin.length(); i += PART_SIZE) {
			int end = i + PART_SIZE;
			if (end > bin.length()) {
				end = (int) bin.length();
			}
			missingparts.add(new DownloadPart(i, end));
		}
	}

	public void stop() {
		log.info("stopping download " + bin);
		giveupflag.trigger();
	}

	@Override
	public void run() {
		log.info("Starting download");

		this.starttime = System.currentTimeMillis();
		flag = new MTimedFlag(P2PServer.DOWNLOAD_RESET_DELAY);
		while (!isReady() && !messenger.isClosed() && !giveupflag.isTriggered()) {
			log.info("reset download ready:" + isReady() + " giveupflag:"
					+ giveupflag);
			flag.reset();
			resetSentStarts();
			sendWhoHasMessage();
			//
			flag.waitTimer();
		}
		//
		server.removeDownload(getID());
		updateSpeedInfo();
		log.info("Download DONE " + isReady() + " " + speedinfo + " source:"
				+ server.isRunning() + " ready:" + isReady() + " giveup: "
				+ giveupflag);
		//
		if(!isReady()) {
			bin.delete();
		}
		
		this.server.reportDownload(getID(), isReady());
	}

	@Override
	public void nodeAdded(WNode n) {
		sendWhoHasMessage(n);
	}

	@Override
	public boolean isDone() {
		return giveupflag.isTriggered() || isReady();
	}

	public void updateSpeedInfo() {
		this.endtime = System.currentTimeMillis();
		this.speedinfo = "Has downloaded " + countbytes + " bytes in "
				+ (endtime - starttime) + " msecs" + "("
				+ (1000.0f * bin.length() / (endtime - starttime)) + " B/s)";
	}

	public synchronized boolean isReady() {
		if (!sentstarts.isEmpty()) {
			return false;
		} else if (this.countbytes < bin.length()) {
			log.info("isready length fail " + countbytes + " binary.length "
					+ bin.length());
			return false;
		} else {
			log.info("isready length ok " + countbytes + " " + bin.length());
			if (!hasBeenReady) {
				bin.flush();
				hasBeenReady = bin.isReady();
			}
			return hasBeenReady;
		}
	}

	private void resetSentStarts() {
		log.info("resetting sent needed pieces");
		sentstarts = new HashMap<Integer, Download.DownloadPart>();
	}

	private void sendWhoHasMessage(WNode n) {
		if (!isReady()) {
			if (n != null) {
				MMessage whoHasMessage = getWhoHasMessage();
				log.info("whohas to node[" + n + "] " + whoHasMessage);
				if (whoHasMessage != null) {
					whoHasMessage.addResponseListener(this);
					n.sendMessage(whoHasMessage);
				} else {
					log.info("Got null WhoHasMessage. is ready?(" + isReady()
							+ ") isDone?(" + isDone() + ")");
				}
			} else {
				sendWhoHasMessage();
			}
		} else {
			flag.trigger();
		}
	}

	@Override
	public void messageReceived(MMessage b) {
		handleResponse(b);
	}

	private void sendWhoHasMessage() {
		if (!isReady()) {
			MMessage m = getWhoHasMessage();
			log.info("broadcasting whohas " + m);
			if (m != null) {
				messenger.broadcastMessage(m, this);
			} else if (!isReady() && sentstarts.isEmpty()) {
				bin.resetCRC();
				log.info("resetCRC " + isReady());
			}
		} else {
			log.info("already ok " + bin);
			flag.trigger();
		}
	}

	public MMessage getWhoHasMessage() {
		synchronized (sentstarts) {
			MMessage m = messenger.getMessage(MESSAGENAME_WHOHAS);
			m.addAttribute("streamid", "" + getID());
			WData needed = m.add("needed");
			if (addNeededPieces(needed) && !isReady()) {
				return m;
			} else {
				return null;
			}
		}
	}

	private void handleResponse(MMessage b) {
		if (b.getName().equals(MESSAGENAME_STREAM) && !isReady()) {
			MStringID sid = b.getIDAttribute("streamid");
			if (sid != null && getID().equals(sid)) {
				handleCheckedMessage(b);
			} else {
				log.error("StreamID " + sid + " while waiting " + bin);
			}
		} else {
			log.info("unknown message " + b);
		}
	}

	private void handleCheckedMessage(MMessage b) {
		log.info("Handling message " + b);

		flag.reset();
		giveupflag.reset();
		//
		byte responsebytes[] = b.getAttachment("bytes");
		if (responsebytes != null) {
			writeRetrievedBytes(b, responsebytes);
		} else {
			log.info("response bytes null");
		}
		//
		String sthrough = b.getAttribute("through");
		WNode lasthandlernode = server.getNode(b.getSentBy());
		if (sthrough != null) {
			MNodeID throughid = new MNodeID(sthrough);
			sendWhoHasMessage(server.getNode(throughid));
		} else if (lasthandlernode != null) {
			sendWhoHasMessage(lasthandlernode);
		} else {
			log.info("not sending back a message... broadcasting later");
		}
		//
		log.debug("stream response handled");
	}

	private void writeRetrievedBytes(MMessage b, byte[] responsebytes) {
		try {
			log.info("Download got " + responsebytes.length + " bytes");
			this.countbytes += responsebytes.length;
			int start = b.getAttributeInt("start");
			int end = b.getAttributeInt("end");
			int length = end - start;
			//
			bin.addAt(start, responsebytes, length);
			//
			removeSentRequestStart(start);
			removeMissingPart(start, responsebytes.length);
			updateSpeedInfo();
		} catch (IOException e) {
			log.error(e);
		}
	}

	private synchronized void removeMissingPart(int start, int length) {
		log.debug("missingparts " + missingparts);

		List<DownloadPart> nlist = new LinkedList<DownloadPart>(missingparts);
		for (int i = 0; i < missingparts.size(); i++) {
			DownloadPart p = missingparts.get(i);
			if (start <= p.start && start + length >= p.end) {
				nlist.remove(i);
				break;
			}
		}
		//
		missingparts = nlist;
	}

	/**
	 * If request for this index has been sent, remove request from sentstarts
	 * -list.
	 * 
	 * @param index
	 */
	private void removeSentRequestStart(int index) {
		synchronized (sentstarts) {
			for (Integer i : new HashSet<Integer>(sentstarts.keySet())) {
				DownloadPart s = sentstarts.get(i);
				if (index >= s.start && index <= s.end) {
					sentstarts.remove(i);
				}
			}
		}
	}

	private BinaryID getID() {
		return bin.getID();
	}

	private synchronized boolean addNeededPieces(WData needed) {
		if (!missingparts.isEmpty()) {
			int randompart = (int) (Math.random() * missingparts.size());
			DownloadPart missingpart = missingparts.get(randompart);

			DownloadPart s = new DownloadPart(missingpart.start,
					missingpart.end);
			sentstarts.put(missingpart.start, s);
			//
			WData p = needed.add("piece");
			p.addValue("start", s.start);
			p.addValue("end", s.end);
			log.info("piece needed " + p);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return "Download[" + bin + "][" + speedinfo + "]";
	}

	private class DownloadPart {
		private int start;
		private int end;

		public DownloadPart(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return "part[" + start + " -> " + end + "]";
		}
	}

}
