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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import waazdoh.client.model.Binary;
import waazdoh.client.model.JBean;
import waazdoh.client.model.MBinaryID;
import waazdoh.client.model.WaazdohInfo;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.MNodeConnection;
import waazdoh.cp2p.network.Node;
import waazdoh.cp2p.network.SourceListener;
import waazdoh.util.MLogger;
import waazdoh.util.MStringID;
import waazdoh.util.MTimedFlag;

public final class Download implements Runnable, MessageResponseListener,
		SourceListener {
	private static final String MESSAGENAME_WHOHAS = "whohas";
	private static final String MESSAGENAME_STREAM = "stream";
	private static final int GIVEUP_TIMEOUT_MSEC = 1000 * 60 * 4;
	private Binary bin;
	private MNodeConnection source;
	private MLogger log;
	private MTimedFlag flag;
	private MTimedFlag giveupflag = new MTimedFlag(GIVEUP_TIMEOUT_MSEC);
	private long endtime;
	private long starttime;
	private Map<Integer, NeededStart> sentstarts = new HashMap<Integer, Download.NeededStart>();
	private boolean hasBeenReady;
	private String speedinfo;
	private int nullcount;
	private int countbytes;
	private int overwritebytes;

	Download(Binary b, MNodeConnection source) {
		this.bin = b;
		this.source = source;
		//
		source.addSourceListener(this);
		//
		Thread t = new Thread(this, "Download");
		t.start();

		log = MLogger.getLogger(this);
		log.info("created");
	}

	public String getMemoryUsageInfo() {
		String info = "";
		info += "sentstarts:[" + sentstarts.size() + "]";
		return info;
	}

	public void stop() {
		log.info("stopping download " + bin);
		giveupflag.trigger();
	}

	@Override
	public void run() {
		this.starttime = System.currentTimeMillis();
		flag = new MTimedFlag(WaazdohInfo.DOWNLOAD_RESET_DELAY);
		while (!isReady() && source.isRunning() && !giveupflag.isTriggered()) {
			log.info("reset download");
			flag.reset();
			resetSentStarts();
			sendWhoHasMessage();
			//
			flag.waitTimer();
		}
		//
		source.removeDownload(getID());
		updateSpeedInfo();
		log.info("Download DONE " + isReady() + " " + speedinfo + " source:"
				+ source.isRunning() + " ready:" + isReady() + " giveup: "
				+ giveupflag);
		//
		this.source.reportDownload(getID(), isReady());
	}

	@Override
	public void nodeAdded(Node n) {
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
			log.info("isready length fail " + countbytes);
			return false;
		} else {
			log.info("isready length ok " + countbytes + " " + bin.length());
			if (!hasBeenReady) {
				hasBeenReady = bin.isReady();
			}
			return hasBeenReady;
		}
	}

	private void resetSentStarts() {
		log.info("resetting sent needed pieces");
		sentstarts = new HashMap<Integer, Download.NeededStart>();
	}

	private void sendWhoHasMessage(Node n) {
		if (!isReady()) {
			if (n != null) {
				MMessage whoHasMessage = getWhoHasMessage();
				log.info("whohas to node[" + n + "] " + whoHasMessage);
				if (whoHasMessage != null) {
					whoHasMessage.addResponseListener(this);
					n.addMessage(whoHasMessage);
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
				source.broadcastMessage(m, this);
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
			MMessage m = source.getMessage(MESSAGENAME_WHOHAS);
			m.addAttribute("streamid", "" + getID());
			JBean needed = m.add("needed");
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
		Node lasthandlernode = source.getNode(b.getSentBy());
		if (sthrough != null) {
			MNodeID throughid = new MNodeID(sthrough);
			sendWhoHasMessage(source.getNode(throughid));
		} else if (lasthandlernode != null) {
			sendWhoHasMessage(lasthandlernode);
		} else {
			log.info("not sending back a message... broadcasting later");
		}
		//
		log.debug("stream response handled");
	}

	private void writeRetrievedBytes(MMessage b, byte[] responsebytes) {
		log.info("Download got floats " + responsebytes.length);
		this.countbytes += responsebytes.length;
		int start = b.getAttributeInt("start");
		//
		overwritebytes += bin.addAt(start, responsebytes);
		//
		removeSentRequestStart(start);
		updateSpeedInfo();
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
				NeededStart s = sentstarts.get(i);
				if (index >= s.start && index <= s.end) {
					sentstarts.remove(i);
				}
			}
		}
	}

	private MBinaryID getID() {
		return bin.getID();
	}

	private synchronized boolean addNeededPieces(JBean needed) {
		while (bin.get(this.nullcount) != null) {
			this.nullcount++;
		}
		int start = nullcount;
		if (start >= bin.length()) {
			// done
			return false;
		} else {
			int getlength = bin.length() - start;
			if (getlength > WaazdohInfo.DOWNLOADER_MAX_REQUESTED_PIECELENGTH) {
				getlength = WaazdohInfo.DOWNLOADER_MAX_REQUESTED_PIECELENGTH;
			}
			//
			Byte[] bytes = bin.getByteBuffer();
			int bytesLength = bin.getBytesLength();
			//
			while (bytes[start] != null || getStartNeedSent(start) != null) {
				NeededStart s = getStartNeedSent(start);
				if (s != null) {
					start = s.end;
				}
				start++;
				if (start >= bytesLength) {
					break;
				}
			}
			//
			int end = start;
			while (end < (bin.length() - 1)
					&& (end >= bytesLength || bytes[end] == null)) {
				end++;
				if (end > bytesLength) {
					end = bin.length() - 1;
				}
			}
			log.info("start:" + start + " end:" + end + " with " + sentstarts
					+ " ow:" + (100.0f * overwritebytes / countbytes));
			if (end > start || (end < bytesLength && bytes[end] == null)) {
				NeededStart s = new NeededStart(start, end);
				sentstarts.put(start, s);
				//
				JBean p = needed.add("piece");
				p.addValue("start", start);
				p.addValue("end", end);
				log.info("piece needed " + p);
				return true;
			} else {
				return false;
			}
		}
	}

	private NeededStart getStartNeedSent(int start) {
		return sentstarts.get(start);
	}

	@Override
	public String toString() {
		return "Download[" + bin + "][" + speedinfo + "]";
	}

	private class NeededStart {
		private int start;
		private int end;

		public NeededStart(int start, int end) {
			this.start = start;
			this.end = end;
		}

		@Override
		public String toString() {
			return "needed[" + start + " -> " + end + "]";
		}
	}

}
