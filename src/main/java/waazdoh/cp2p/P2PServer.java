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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import waazdoh.client.BinarySource;
import waazdoh.client.ReportingService;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.WData;
import waazdoh.client.model.objects.Binary;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageFactory;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.cp2p.messaging.MessageID;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.MMessager;
import waazdoh.cp2p.network.MNodeConnection;
import waazdoh.cp2p.network.PassiveNode;
import waazdoh.cp2p.network.SourceListener;
import waazdoh.cp2p.network.TCPListener;
import waazdoh.cp2p.network.TCPNode;
import waazdoh.cp2p.network.WNode;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;
import waazdoh.util.MTimedFlag;

public final class P2PServer implements MMessager, MMessageFactory,
		MNodeConnection {
	private static final int MINIMUM_TIMEOUT = 10;
	static final int MESSAGESENDLOOP_COUNT = 1;
	private static final int MAX_SENTCOUNT = 2;
	private static final long REBOOT_DELAY = 120000;
	//
	private MLogger log = MLogger.getLogger(this);
	private final Map<MStringID, Download> downloads = new HashMap<MStringID, Download>();
	private MNodeID networkid;
	private Map<String, MMessageHandler> handlers = new HashMap<String, MMessageHandler>();
	//
	private List<WNode> nodes = new LinkedList<WNode>();
	private Set<SourceListener> sourcelisteners = new HashSet<SourceListener>();
	private Map<MessageID, MessageResponseListener> responselisteners = new HashMap<MessageID, MessageResponseListener>();
	//
	boolean dobind;
	private boolean closed = false;
	private MPreferences p;
	private long lastmessagereceived;
	private TCPListener tcplistener;

	private long bytecountstart = System.currentTimeMillis();
	private int inputbytecount;
	private long outputbytecount;

	private ThreadGroup tg = new ThreadGroup("p2p");
	private ReportingService reporting;
	private BinarySource binarysource;

	final private Map<WNode, NodeStatus> nodestatuses = new HashMap<WNode, NodeStatus>();

	public P2PServer(MPreferences p, boolean bind2, BinarySource nbinsource) {
		this.p = p;
		this.dobind = bind2;
		this.binarysource = nbinsource;
		initHandlers();
		//
		runChecker();
	}

	public void setReportingService(ReportingService reporting) {
		this.reporting = reporting;
	}

	@Override
	public void reportDownload(MStringID id, boolean success) {
		if (reporting != null) {
			reporting.reportDownload(id, success);
		}
	}

	public String getInfoText() {
		List<WNode> ns = nodes;
		if (ns != null) {
			String s = "nodes:" + ns.size() + " downloads:" + downloads.size();
			s += " " + tcplistener + " messagereceived:"
					+ (System.currentTimeMillis() - lastmessagereceived)
					+ "ms ago ";
			long dtime = System.currentTimeMillis() - bytecountstart;
			if (dtime > 0) {
				s += " I:" + (inputbytecount * 1000 / dtime);
				s += " O:" + (outputbytecount * 1000 / dtime);
			}
			//
			if (dtime > 10 * 1000) {
				bytecountstart = System.currentTimeMillis();
				inputbytecount = 0;
				outputbytecount = 0;
			}
			return s;
		} else {
			return "closed";
		}
	}

	public String getMemoryUserInfo() {
		String info = "";

		info += "downloads:" + downloads.size();
		info += "[";
		for (Download download : downloads.values()) {
			info += download.getMemoryUsageInfo();
		}
		info += "]";

		return info;
	}

	private void initHandlers() {
		handlers.put("ping", new PingHandler());
		HelloHandler helloh = new HelloHandler();
		handlers.put(HelloHandler.HELLO, helloh);
		handlers.put(HelloHandler.HOLA, helloh);

		WhoHasHandler whohashandler = new WhoHasHandler(binarysource, this);
		whohashandler.addListener(new WhoHasListener() {
			@Override
			public void binaryRequested(BinaryID streamid, Integer count) {
				Download download = getDownload(streamid);
				if (download == null) {
					binarysource.getOrDownload(streamid);
				} else {
					log.info("already downloading " + download);
				}
			}
		});

		handlers.put("whohas", whohashandler);
		//
		for (MMessageHandler handler : handlers.values()) {
			handler.setFactory(this);
		}
	}

	public void addMessageHandler(String name, MMessageHandler h) {
		handlers.put(name, h);
	}

	public void setDownloadEverything(boolean b) {
		WhoHasHandler handler = (WhoHasHandler) handlers.get("whohas");
		handler.downloadEveryThing(b);
	}

	private void runChecker() {
		final Thread t = new Thread(tg, new Runnable() {
			@Override
			public void run() {
				lastmessagereceived = System.currentTimeMillis();
				while (isRunning()) {
					long dt = System.currentTimeMillis() - lastmessagereceived;
					if (dt > REBOOT_DELAY) {
						reboot();
					} else {
						waitNodes(dt - REBOOT_DELAY);
					}
				}
				log.info("Reboot checker out");
			}
		}, "P2PServer checker");
		t.start();
	}

	private void reboot() {
		try {
			log.info("rebooting server "
					+ (REBOOT_DELAY - System.currentTimeMillis()));
			shutdown();
			startNetwork();
		} catch (Exception e) {
			log.error(e);
		}
	}

	public void startNetwork() {
		if (!isClosed()) {
			log.info("Starting network");
			//
			lastmessagereceived = System.currentTimeMillis();
			if (networkid == null) {
				networkid = new MNodeID(new MStringID());
			}
			//
			if (dobind) {
				tcplistener = new TCPListener(tg, this, p);
				tcplistener.start();
			}

			startMessageSendLoops();
		} else {
			log.info("Not starting the network. Closed.");
		}
	}

	public WNode addNode(MHost string, int i) {
		log.info("addnode " + string + " " + i);
		WNode n = new TCPNode(string, i, this);
		addNode(n);
		return n;
	}

	WNode addNode(MNodeID nodeid) {
		WNode n = new PassiveNode(nodeid, this);
		addNode(n);
		return n;
	}

	public void addNode(WNode n) {
		log.info("adding node " + n);
		if (nodes != null) {
			synchronized (nodes) {
				nodes.add(n);
				log.info("node added " + n);

				NodeStatus status = new NodeStatus();
				nodestatuses.put(n, status);
			}
			//
			Set<SourceListener> listeners = sourcelisteners;
			for (SourceListener sourceListener : listeners) {
				sourceListener.nodeAdded(n);
			}
		}
	}

	void startMessageSendLoops() {
		int loopcount = MESSAGESENDLOOP_COUNT;
		for (int i = 0; i < loopcount; i++) {
			Thread t = new Thread(tg, new Runnable() {
				@Override
				public void run() {
					messageSendLoop();
				}
			}, "MessageSendLooper");
			t.start();
		}
	}

	private void messageSendLoop() {
		while (isRunning()) {
			WNode node = null;
			Iterator<WNode> iterator;
			iterator = getNodesIterator();
			//
			while (iterator.hasNext()) {
				node = iterator.next();
				NodeStatus nodestatus = getNodeStatus(node);

				if (node.isConnected() && nodestatus.checkPing()) {
					sendPing(node);
				}
				//
				if (node.getID() != null && node.getID().equals(networkid)) {
					log.info("Having myself as remote node. Removing.");
					synchronized (nodes) {
						node.close();
						nodes.remove(node);
					}
				} else if (getNodeStatus(node).shouldDie() || node.isClosed()) {
					log.info("Removing node " + node + " nodes:" + nodes);
					if (nodes != null) {
						List<WNode> ns = nodes;
						synchronized (ns) {
							node.close();
							ns.remove(node);
							nodestatuses.remove(node);
						}
					}
				}
			}

			checkDefaultNodes();

			int timeout = 100 + (int) (Math.random() * 100 * nodes.size() * MESSAGESENDLOOP_COUNT);
			waitNodes(timeout);
		}
		log.info("server thread shutting down");
	}

	private void waitNodes(long timeout) {
		if (timeout < MINIMUM_TIMEOUT) {
			timeout = MINIMUM_TIMEOUT;
		}

		List<WNode> ns = this.nodes;
		if (ns != null) {
			synchronized (ns) {
				try {
					ns.wait(timeout);
				} catch (InterruptedException e) {
					log.error(e);
				}
			}
		}
	}

	public NodeStatus getNodeStatus(WNode node) {
		return nodestatuses.get(node);
	}

	private void checkDefaultNodes() {
		try {
			synchronized (nodes) {
				if (nodes.isEmpty()) {
					addDefaultNodes();
					if (nodes.isEmpty()) {
						int maxwaittime = 5000;
						log.info("nodes size still zero. Waiting "
								+ maxwaittime + "msec");
						waitForConnection(maxwaittime);
					}
				}
			}
		} catch (InterruptedException ie) {
			log.error(ie);
		}
	}

	private void sendPing(WNode node) {
		MMessage message = getMessage("ping");
		addResponseListener(message.getID(), new MessageResponseListener() {
			private long sent = System.currentTimeMillis();
			private boolean done = false;

			@Override
			public void messageReceived(MMessage message) {
				log.info("PING response in "
						+ (System.currentTimeMillis() - sent) + " ms");
				done = true;
			}

			@Override
			public boolean isDone() {
				if ((System.currentTimeMillis() - sent) > 10000) {
					log.info("PING giving up");
					return true;
				} else {
					return done;
				}
			}
		});

		node.sendMessage(message);
		getNodeStatus(node).pingSent();
	}

	private Iterator<WNode> getNodesIterator() {
		Iterator<WNode> iterator;
		List<WNode> ns = this.nodes;
		synchronized (nodes) {
			if (ns.isEmpty()) {
				addDefaultNodes();
			}
			iterator = new LinkedList<WNode>(ns).iterator();
		}
		return iterator;
	}

	private void addDefaultNodes() {
		new ConditionWaiter(new ConditionWaiter.Condition() {
			@Override
			public boolean test() {
				String slist = p.get(MPreferences.SERVERLIST, "");
				return slist != null && slist.length() > 0;
			}
		}, 2000);

		String slist = p.get(MPreferences.SERVERLIST, "");
		log.info("got server list " + slist);

		if (slist == null || slist.length() == 0) {
			slist = "";
			log.info("Serverlist empty. Adding service domain with default port");
			String service = p.get(MPreferences.SERVICE_URL, "");
			URL u;
			try {
				u = new URL(service);
				String host = u.getHost();
				log.info("host " + host);
				slist = host + ":" + TCPListener.DEFAULT_PORT;
				log.info("new list " + slist);
			} catch (MalformedURLException e) {
				log.error(e);
			}
		}

		//
		StringTokenizer st = new StringTokenizer(slist, ",");
		while (st.hasMoreTokens()) {
			String server = st.nextToken();
			int indexOf = server.indexOf(':');
			if (indexOf > 0) {
				String host = server.substring(0, indexOf);
				int port = Integer.parseInt(server.substring(indexOf + 1));
				addNode(new MHost(host), port);
			} else {
				log.info("invalid value " + server);
			}
		}
	}

	public void notifyNodes() {
		if (nodes != null) {
			try {
				synchronized (nodes) {
					nodes.notifyAll();
				}
			} catch (IllegalMonitorStateException e) {
				// TODO not sure what happens here or if it is still happening.
				// Logging added.
				log.error(e);
			}
		}
	}

	public void broadcastMessage(MMessage notification) {
		broadcastMessage(notification, null);
	}

	@Override
	public void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener) {
		broadcastMessage(notification, messageResponseListener, null);
	}

	@Override
	public void addSourceListener(SourceListener nodeListener) {
		sourcelisteners.add(nodeListener);
	}

	public void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions) {
		if (nodes == null) {
			return;
		}
		//
		if (notification.getSentCount() <= MAX_SENTCOUNT) {
			sendToNodes(notification, messageResponseListener, exceptions);
		} else {
			log.info("not sending m" + "essage " + notification
					+ " due sentcount " + notification.getSentCount());
		}
	}

	private void sendToNodes(MMessage notification,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions) {
		notification.addAttribute("sentcount", notification.getSentCount() + 1);
		synchronized (nodes) {
			for (WNode node : nodes) {
				if (node.isConnected()) {
					if (exceptions == null
							|| !exceptions.contains(node.getID())) {
						node.sendMessage(notification);
						addResponseListener(notification.getID(),
								messageResponseListener);
					} else {
						log.debug("Not broadcasting to " + node
								+ " because node is in exceptions list");
					}
				} else {
					log.debug("not broadcasting to " + node
							+ " node because it's not yet connected");
				}
			}
			//
			nodes.notify();
		}
	}

	public boolean isRunning() {
		return !closed || nodes != null;
	}

	public void close() {
		log.info("closing server");
		startClosing();
		//
		shutdown();
		nodes = null;

		log.info("closing done");
	}

	public void startClosing() {
		log.info("starting closing");
		closed = true;

		if (tcplistener != null) {
			tcplistener.startClosing();
		}

		synchronized (downloads) {
			for (Download d : downloads.values()) {
				d.stop();
			}
		}
	}

	private void shutdown() {
		log.info("shutting down");
		shutdownNodes();

		closeListener();
		//
		if (nodes != null) {
			notifyNodes();
			log.info("closing nodes again");
			List<WNode> ns = new LinkedList<>(nodes);
			nodes = new LinkedList<WNode>();

			//
			for (WNode node : ns) {
				node.close();
			}
		}
		//
		log.info("shutdown complete");
	}

	private void closeListener() {
		log.info("closing tcplistener " + tcplistener);
		if (tcplistener != null) {
			tcplistener.close();
			tcplistener = null;
		}
	}

	private void shutdownNodes() {
		log.info("closing nodes");
		if (nodes != null) {
			List<WNode> ns = new LinkedList<WNode>(nodes);
			for (WNode n : ns) {
				n.close();
			}
		}
	}

	@Override
	public List<MMessage> handle(List<MMessage> messages) {
		if (!isClosed()) {
			lastmessagereceived = System.currentTimeMillis();
			//
			List<MMessage> ret;
			ret = new LinkedList<MMessage>();
			//
			WNode sentbynode = null; // should be same node for every message
			WNode lasthandler = null;
			for (MMessage message : messages) {
				MNodeID lasthandlerid = message.getLastHandler();
				lasthandler = getNode(lasthandlerid);

				MNodeID sentby = message.getSentBy();
				if (!networkid.equals(sentby)) {
					//
					sentbynode = getNode(sentby);
					if (sentbynode == null) {
						sentbynode = addNode(sentby);
					}
					//

					getNodeStatus(sentbynode)
							.messageReceived(message.getName());
					//
					log.info("handling message: " + message + " from "
							+ sentbynode);
					message.setLastHandler(networkid);
					//
					handle(message, lasthandler != null ? lasthandler
							: sentbynode);
				} else {
					log.debug("not handling message because networkid is equal with sentby "
							+ message.getSentBy());
					ret.add(getMessage("close"));
				}
			}

			if (lasthandler != null) {
				ret = lasthandler.getOutgoingMessages();
			} else if (sentbynode != null) {
				ret = sentbynode.getOutgoingMessages();
			}
			log.info("" + messages.size() + " handled and returning " + ret);
			return ret;
		} else {
			log.info("Closed. Not handling message, returning null");
			return null;
		}
	}

	private boolean isClosed() {
		return closed;
	}

	public void handle(MMessage message, WNode node) {
		try {
			inputbytecount += message.getByteCount();
			log.info(this.toString() + " " + message);
			//
			WData nodeinfo = message.get("nodeinfo");
			if (nodeinfo != null) {
				handleNodeInfo(nodeinfo);
			}
			//
			MessageID responseto = message.getResponseTo();
			MessageResponseListener responselistener = getResponseListener(responseto);
			if (responselistener != null) {
				responselistener.messageReceived(message);
				removeResponseListener(responseto);
			} else {
				MNodeID to = message.getTo("to");
				if (to == null || to.equals(getID())) {
					handleMyMessage(message, node);
				} else {
					redirectMessageTo(message, node, to);
				}
			}
		} catch (Exception e) {
			log.error(e);
			getNodeStatus(node).warning();
		}
	}

	private void redirectMessageTo(MMessage message, WNode node, MNodeID to) {
		WNode tonode = getNode(to);
		log.info("redirecting message to " + tonode);
		if (tonode != null) {
			node.addInfoTo(message);
			tonode.sendMessage(message);
		} else {
			log.error("message received in wrong node " + message);
		}
	}

	private void handleMyMessage(MMessage message, WNode node) {
		MMessageHandler handler = getHandler(message.getName());
		if (handler != null) {
			MMessage returnmessage = handler.handle(message);
			if (returnmessage != null) {
				node.sendMessage(returnmessage);
				addResponseListener(returnmessage);
			}
		} else {
			log.error("unknown message " + message);
		}
	}

	private void handleNodeInfo(WData nodeinfo) {
		List<WData> nodeinfos = nodeinfo.getChildren();
		for (WData inode : nodeinfos) {
			MNodeID nodeinfoid = new MNodeID(inode.getName());
			if (getNode(nodeinfoid) == null) {
				MHost host = new MHost(inode.getValue("host"));
				int port = inode.getIntValue("port");
				addNode(host, port);
			}
		}
	}

	private MMessageHandler getHandler(final String name) {
		return handlers.get(name);
	}

	@Override
	public MMessage newResponseMessage(MMessage childb, String string) {
		MMessage m = getMessage(string);
		m.addAttribute("responseto", childb.getAttribute("messageid"));
		m.addIDAttribute("to", childb.getSentBy());
		return m;
	}

	@Override
	public MMessage getMessage(final String string) {
		MMessage ret = new MMessage(string, networkid);
		ret.addAttribute("date", "" + new Date());
		return ret;
	}

	public WNode getNode(MNodeID sentby) {
		if (sentby != null) {
			for (WNode node : nodes) {
				if (sentby.equals(node.getID())) {
					return node;
				}
			}
		}
		return null;
	}

	private void removeResponseListener(MessageID id) {
		synchronized (responselisteners) {
			responselisteners.remove(id);
		}

	}

	private void addResponseListener(MMessage returnmessage) {
		MessageResponseListener l = returnmessage.getResponseListener();
		if (l != null) {
			addResponseListener(returnmessage.getID(), l);
		}
	}

	public void addResponseListener(MessageID id,
			MessageResponseListener messageResponseListener) {
		synchronized (responselisteners) {
			if (messageResponseListener != null
					&& responselisteners.get(id) == null) {
				responselisteners.put(id, messageResponseListener);
			} else {
				log.debug("not adding responselistener " + id + " -> "
						+ messageResponseListener);
			}
		}
	}

	private MessageResponseListener getResponseListener(MessageID id) {
		synchronized (responselisteners) {
			return responselisteners.get(id);
		}
	}

	public void nodeConnected(WNode node) {
		MMessage m = new MMessage("connectednode", getID());
		Set<MNodeID> exceptions = new HashSet<MNodeID>();
		exceptions.add(node.getID());
		broadcastMessage(m, null, exceptions);
	}

	public MNodeID getID() {
		return networkid;
	}

	public Download getDownload(MStringID streamid) {
		return downloads.get(streamid);
	}

	@Override
	public void removeDownload(MStringID did) {
		synchronized (downloads) {
			log.info("removing download " + did);
			if (getDownload(did) != null) {
				getDownload(did).stop();
				downloads.remove(did);
			}
		}
	}

	public void addDownload(Binary bs) {
		synchronized (downloads) {
			if (downloads.get(bs.getID()) == null) {
				log.info("adding download " + bs + " memory:"
						+ getMemoryUserInfo());
				downloads.put(bs.getID(), new Download(bs, this));
			}
		}
	}

	public void start() {
		startNetwork();
	}

	public boolean canDownload() {
		return downloads.size() < p.getInteger(
				MPreferences.NETWORK_MAX_DOWNLOADS,
				MPreferences.NETWORK_MAX_DOWNLOADS_DEFAULT);
	}

	public void clearMemory() {
		synchronized (responselisteners) {
			Map<MessageID, MessageResponseListener> nresponselisteners = new HashMap<MessageID, MessageResponseListener>(
					responselisteners);
			for (MessageID id : nresponselisteners.keySet()) {
				MessageResponseListener l = getResponseListener(id);
				if (l.isDone()) {
					removeResponseListener(id);
				}
			}
			//
			synchronized (sourcelisteners) {
				List<SourceListener> nsourcelisteners = new LinkedList<SourceListener>(
						sourcelisteners);
				for (SourceListener l : nsourcelisteners) {
					if (l.isDone()) {
						sourcelisteners.remove(l);
					}
				}
			}

			synchronized (downloads) {
				Map<MStringID, Download> ndownloads = new HashMap<MStringID, Download>(
						downloads);
				for (MStringID did : ndownloads.keySet()) {
					Download d = getDownload(did);
					if (d.isDone()) {
						removeDownload(did);
					}
				}

			}
		}
		//
	}

	public boolean waitForDownloadSlot(final int i) {
		try {
			int waittime = i;
			while (!canDownload() && waittime > 0) {
				synchronized (this) {
					this.wait(100);
					waittime -= 100;
				}
			}
		} catch (InterruptedException e) {
			log.error(e);
		}

		return canDownload();

	}

	@Override
	public String toString() {
		return "P2PServer[" + getID() + " " + this.getInfoText() + "]";
	}

	public boolean isConnected() {
		if (nodes != null) {
			List<WNode> ns = new LinkedList<WNode>(this.nodes);
			for (WNode node : ns) {
				if (node.isConnected()
						&& getNodeStatus(node).getReceivedMessages() > 0) {
					return true;
				}
			}
		}
		return false;
	}

	public void waitForConnection(int maxwaittime) throws InterruptedException {
		MTimedFlag timer = new MTimedFlag(maxwaittime);
		while (isRunning() && !isConnected()) {
			synchronized (nodes) {
				nodes.wait(100);
				if (timer.isTriggered()) {
					log.info("waitForConnection failed. Returning.");
					return;
				}
			}
		}
	}

	public int getPort() {
		return tcplistener.getPort();
	}

	public MPreferences getPreferences() {
		return p;
	}

	public class NodeStatus {
		public static final long MAX_DIE_TIME = 30100;
		public static final int WARNING_TRESHOLD = 5;
		public static final long MAX_PINGDELAY = 10000;
		public static final long MIN_PINGDELAY = 200;

		private long lastping = System.currentTimeMillis();
		private long currentpingdelay;
		private long touch = System.currentTimeMillis();
		private int warning;
		private int receivedmessages;

		public boolean checkPing() {
			long maxpingdelay = getPingDelay();
			if (System.currentTimeMillis() - lastping > maxpingdelay) {
				log.debug("should ping "
						+ (System.currentTimeMillis() - lastping) + " > "
						+ maxpingdelay);
				lastping = System.currentTimeMillis();
				return true;
			} else {
				return false;
			}

		}

		private long getPingDelay() {
			if (this.currentpingdelay < MIN_PINGDELAY) {
				currentpingdelay = MIN_PINGDELAY;
			}
			return this.currentpingdelay;
		}

		public void pingSent() {
			this.currentpingdelay = getPingDelay() * 2;
			if (currentpingdelay > NodeStatus.MAX_PINGDELAY) {
				currentpingdelay = NodeStatus.MAX_PINGDELAY;
			}
		}

		public void messageReceived(String name) {
			receivedmessages++;
			touch();

			if (name.toLowerCase().indexOf("ping") < 0) {
				// received ping messages do not effect time between pings.
				this.currentpingdelay /= 2;
			}
			log.info("Message received " + receivedmessages
					+ " current ping delay:" + this.getPingDelay() + "ms");
		}

		public void touch() {
			this.touch = System.currentTimeMillis();
		}

		public void warning() {
			this.warning++;
		}

		public boolean shouldDie() {
			return (System.currentTimeMillis() - touch) > MAX_DIE_TIME
					|| warning > WARNING_TRESHOLD;
		}

		public int getReceivedMessages() {
			return receivedmessages;
		}
	}
}
