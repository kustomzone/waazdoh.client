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

import waazdoh.client.binaries.ReportingService;
import waazdoh.client.model.Binary;
import waazdoh.client.model.JBean;
import waazdoh.client.model.MBinaryID;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageFactory;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.cp2p.messaging.MMessageList;
import waazdoh.cp2p.messaging.MessageID;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.MMessager;
import waazdoh.cp2p.network.MNodeConnection;
import waazdoh.cp2p.network.Node;
import waazdoh.cp2p.network.SourceListener;
import waazdoh.cp2p.network.TCPListener;
import waazdoh.util.ConditionWaiter;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;
import waazdoh.util.MTimedFlag;

public final class P2PServer implements MMessager, MMessageFactory,
		MNodeConnection {
	static final int MESSAGESENDLOOP_COUNT = 10;
	private static final int MAX_SENTCOUNT = 2;
	private static final long REBOOT_DELAY = 120000;
	//
	private MLogger log = MLogger.getLogger(this);
	private final Map<MStringID, Download> downloads = new HashMap<MStringID, Download>();
	MStringID networkid;
	Map<String, MMessageHandler> handlers = new HashMap<String, MMessageHandler>();
	//
	List<Node> nodes = new LinkedList<Node>();
	Set<SourceListener> sourcelisteners = new HashSet<SourceListener>();
	private Map<MessageID, MessageResponseListener> responselisteners = new HashMap<MessageID, MessageResponseListener>();
	//
	boolean dobind;
	private boolean closed = false;
	private MPreferences p;
	private ByteArraySource bytesource;
	private long lastmessagereceived;
	private TCPListener tcplistener;

	private long bytecountstart = System.currentTimeMillis();
	private int inputbytecount;
	private long outputbytecount;

	private ThreadGroup tg = new ThreadGroup("p2p");
	private ReportingService reporting;

	public P2PServer(MPreferences p, boolean bind2, ByteArraySource nbytesource) {
		this.p = p;
		this.dobind = bind2;
		this.bytesource = nbytesource;
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
		if (nodes != null) {
			String s = "nodes:" + nodes.size() + " downloads:"
					+ downloads.size();
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

		for (Node node : nodes) {
			info += " node:" + node.getMemoryUsageInfo();
		}
		return info;
	}

	private void initHandlers() {
		handlers.put("ping", new PingHandler());
		WhoHasHandler whohashandler = new WhoHasHandler(bytesource, this);
		whohashandler.addListener(new WhoHasListener() {
			@Override
			public void binaryRequested(MBinaryID streamid, Integer count) {
				Download download = getDownload(streamid);
				if (download == null) {
					bytesource.addDownload(streamid);
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

	private void sleep(long time) {
		synchronized (this) {
			try {
				if (time < 10) {
					time = 10;
				}
				this.wait(time);
			} catch (InterruptedException e) {
				log.error(e);
			}
		}
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
						sleep(dt - REBOOT_DELAY);
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
				networkid = new MStringID();
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

	public Node addNode(MHost string, int i) {
		log.info("addnode " + string + " " + i);
		Node n = new Node(null, string, i, this);
		addNode(n);
		return n;
	}

	Node addNode(MNodeID nodeid, MHost host, int port) {
		Node n = new Node(nodeid, host, port, this);
		addNode(n);
		return n;
	}

	public void addNode(Node n) {
		log.info("adding node " + n);
		synchronized (nodes) {
			nodes.add(n);
			log.info("node added " + n);
		}
		//
		Set<SourceListener> listeners = sourcelisteners;
		for (SourceListener sourceListener : listeners) {
			sourceListener.nodeAdded(n);
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
			Node node = null;
			Iterator<Node> iterator;
			iterator = getNodesIterator();
			//
			while (iterator.hasNext()) {
				node = iterator.next();
				if (node.checkPing()) {
					sendPing(node);
				}
				//
				if (node.getID() != null && node.getID().equals(networkid)) {
					log.info("Having myself as remote node. Removing.");
					synchronized (nodes) {
						node.close();
						nodes.remove(node);
					}
				} else if (node.shouldDie()) {
					log.info("Killing node " + node);
					synchronized (nodes) {
						node.close();
						nodes.remove(node);
					}
				} else {
					node.check();
				}
			}
			if (nodes != null) {
				synchronized (nodes) {
					try {
						checkDefaultNodes();
						//
						nodes.wait(100 + (int) (Math.random() * 100
								* nodes.size() * MESSAGESENDLOOP_COUNT));
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
			}
		}
		log.info("server thread shutting down");
	}

	private void checkDefaultNodes() throws InterruptedException {
		if (nodes.size() == 0) {
			addDefaultNodes();
			if (nodes.size() == 0) {
				int maxwaittime = 5000;
				log.info("nodes size still zero. Waiting " + maxwaittime
						+ "msec");
				waitForConnection(maxwaittime);
			}
		}
	}

	private void sendPing(Node node) {
		MMessage message = getMessage("ping");
		node.addMessage(message);
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
		node.pingSent();
	}

	private Iterator<Node> getNodesIterator() {
		Iterator<Node> iterator;
		synchronized (nodes) {
			if (nodes.size() == 0) {
				addDefaultNodes();
			}
			iterator = new LinkedList<Node>(this.nodes).iterator();
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
				// TODO not sure what happens here.
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
			for (Node node : nodes) {
				if (node.isConnected()) {
					if (exceptions == null
							|| !exceptions.contains(node.getID())) {
						node.addMessage(notification);
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
		return !closed;
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

		for (Download d : downloads.values()) {
			d.stop();
		}
	}

	private void shutdown() {
		log.info("shutting down");
		shutdownNodes();

		log.info("closing tcplistener " + tcplistener);
		if (tcplistener != null) {
			tcplistener.close();
			tcplistener = null;
		}
		//
		if (nodes != null) {
			notifyNodes();
			log.info("closing nodes again");
			List<Node> ns = new LinkedList<>(nodes);
			nodes = new LinkedList<Node>();

			//
			for (Node node : ns) {
				node.close();
			}
		}
		//
		log.info("shutdown complete");
	}

	private void shutdownNodes() {
		log.info("closing nodes");
		if (nodes != null) {
			List<Node> ns = new LinkedList<Node>(nodes);
			for (Node n : ns) {
				n.close();
			}
		}
	}

	@Override
	public MMessageList handle(MMessageList messages) {
		if (!isClosed()) {
			lastmessagereceived = System.currentTimeMillis();
			//
			MMessageList ret;
			ret = new MMessageList();
			//
			Node sentbynode = null; // should be same node for every message
			Node lasthandler = null;
			for (MMessage message : messages) {
				MNodeID lasthandlerid = message.getLastHandler();
				lasthandler = getNode(lasthandlerid);

				MNodeID sentby = message.getSentBy();
				if (!networkid.equals(sentby)) {
					//
					sentbynode = getNode(sentby);
					if (sentbynode == null) {
						sentbynode = new Node(sentby, this);
						addNode(sentbynode);
					}
					//
					sentbynode.touch();
					sentbynode.messageReceived(message.getName());
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
				ret = lasthandler.getMessages();
			} else if (sentbynode != null) {
				ret = sentbynode.getMessages();
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

	public void handle(MMessage message, Node node) {
		try {
			inputbytecount += message.getByteCount();
			log.info(this.toString() + " " + message);
			MMessageHandler handler = getHandler(message.getName());
			//
			JBean nodeinfo = message.get("nodeinfo");
			if (nodeinfo != null) {
				List<JBean> nodeinfos = nodeinfo.getChildren();
				for (JBean inode : nodeinfos) {
					MNodeID nodeinfoid = new MNodeID(inode.getName());
					if (getNode(nodeinfoid) == null) {
						MHost host = new MHost(inode.getValue("host"));
						int port = inode.getIntValue("port");
						addNode(nodeinfoid, host, port);
					}
				}
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
					if (handler != null) {
						MMessage returnmessage = handler.handle(message);
						if (returnmessage != null) {
							node.addMessage(returnmessage);
							addResponseListener(returnmessage);
						}
					} else {
						log.error("unknown message " + message);
					}
				} else {
					Node tonode = getNode(to);
					log.info("redirecting message to " + tonode);
					if (tonode != null) {
						node.addInfoTo(message);
						tonode.addMessage(message);
					} else {
						log.error("message received in wrong node " + message);
					}
				}
			}
		} catch (Exception e) {
			log.error(e);
			node.warning();
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

	public Node getNode(MNodeID sentby) {
		if (sentby != null) {
			for (Node node : nodes) {
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
			if (messageResponseListener != null) {
				responselisteners.put(id, messageResponseListener);
			}
		}
	}

	private MessageResponseListener getResponseListener(MessageID id) {
		synchronized (responselisteners) {
			return responselisteners.get(id);
		}
	}

	public void nodeConnected(Node node) {
		MMessage m = new MMessage("connectednode", getID());
		Set<MNodeID> exceptions = new HashSet<MNodeID>();
		exceptions.add(node.getID());
		broadcastMessage(m, null, exceptions);
	}

	public MStringID getID() {
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

	public void clearMemory(int suggestedmemorytreshold) {
		synchronized (responselisteners) {
			HashMap<MessageID, MessageResponseListener> nresponselisteners = new HashMap<MessageID, MessageResponseListener>(
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

	public boolean waitForDownloadSlot(int i) {
		try {
			while (!canDownload() && i > 0) {
				synchronized (this) {
					this.wait(100);
					i -= 100;
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
			List<Node> ns = new LinkedList<Node>(this.nodes);
			for (Node node : ns) {
				if (node.isConnected()) {
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
}
