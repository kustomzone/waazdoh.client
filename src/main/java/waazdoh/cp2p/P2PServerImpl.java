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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import waazdoh.client.BinarySource;
import waazdoh.client.ReportingService;
import waazdoh.client.model.BinaryID;
import waazdoh.client.model.objects.Binary;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.common.MStringID;
import waazdoh.common.MTimedFlag;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.PassiveNode;
import waazdoh.cp2p.network.ServerListener;
import waazdoh.cp2p.network.TCPListener;
import waazdoh.cp2p.network.TCPNode;
import waazdoh.cp2p.network.WMessengerImpl;
import waazdoh.cp2p.network.WNode;

public final class P2PServerImpl implements P2PServer {
	private static final int MINIMUM_TIMEOUT = 10;
	static final int NODECHECKLOOP_COUNT = 3;
	private static final long REBOOT_DELAY = 120000;
	//
	private WLogger log = WLogger.getLogger(this);
	private final Map<MStringID, Download> downloads = new HashMap<MStringID, Download>();
	//
	private List<WNode> nodes = new LinkedList<WNode>();
	private Set<ServerListener> listeners = new HashSet<ServerListener>();
	//
	private boolean closed = false;
	private WPreferences p;
	private TCPListener tcplistener;

	private ThreadGroup nodechecktg;
	private ReportingService reporting;
	private BinarySource binarysource;

	private final Map<WNode, NodeStatus> nodestatuses = new HashMap<WNode, NodeStatus>();
	private final WMessenger messenger;
	private boolean dobind;
	private Thread rebootchecker;

	public P2PServerImpl(WPreferences p, boolean bind2) {
		this.p = p;
		this.dobind = bind2;
		messenger = new WMessengerImpl(this, p);
		nodechecktg = new ThreadGroup("p2p_" + messenger.getID());
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

	private synchronized void startNetwork() {
		addHandlers();

		if (dobind) {
			tcplistener = new TCPListener(getMessenger(), p);
			tcplistener.start();
		}

		runChecker();
		startNodeCheckLoops();
	}

	private void addHandlers() {
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

		getMessenger().addMessageHandler(WhoHasHandler.MESSAGENAME,
				whohashandler);
	}

	private synchronized void runChecker() {
		if (rebootchecker == null) {
			rebootchecker = new Thread(nodechecktg, new Runnable() {
				@Override
				public void run() {
					try {
						loopChecker();
					} finally {
						rebootchecker = null;
					}
				}
			}, "P2PServer checker");
			rebootchecker.start();
		}
	}

	@Override
	public void setBinarySource(BinarySource storage) {
		this.binarysource = storage;
	}

	@Override
	public void setReportingService(ReportingService reporting) {
		this.reporting = reporting;
	}

	@Override
	public void reportDownload(MStringID id, boolean success) {
		if (reporting != null) {
			reporting.reportDownload(id, success);
		}
	}

	@Override
	public String getInfoText() {
		List<WNode> ns = nodes;
		if (ns != null) {
			String s = "nodes:" + ns.size() + " downloads:" + downloads.size();
			s += " " + tcplistener + " messenger:"
					+ getMessenger().getInfoText();
			return s;
		} else {
			return "closed";
		}
	}

	@Override
	public WNode addNode(MHost string, int i) {
		log.info("addnode " + string + " " + i);
		WNode n = new TCPNode(string, i, getMessenger());
		addNode(n);
		return n;
	}

	@Override
	public WNode addNode(MNodeID nodeid) {
		WNode n = new PassiveNode(nodeid, getMessenger());
		addNode(n);
		return n;
	}

	public synchronized void addNode(WNode n) {
		log.info("adding node " + n);
		if (nodes != null) {
			nodes.add(n);
			log.info("node added " + n);

			NodeStatus status = new NodeStatus();
			nodestatuses.put(n, status);
			//
			synchronized (listeners) {
				for (ServerListener sourceListener : listeners) {
					sourceListener.nodeAdded(n);
				}
			}
		}
	}

	void startNodeCheckLoops() {
		int loopcount = NODECHECKLOOP_COUNT;
		for (int i = 0; i < loopcount; i++) {
			Thread t = new Thread(nodechecktg, new Runnable() {
				@Override
				public void run() {
					try {
						messageNodeCheckLoop();
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
			}, "MessageSendLooper");
			t.start();
		}
	}

	private synchronized void messageNodeCheckLoop()
			throws InterruptedException {
		while (isRunning() && nodechecktg.activeCount() <= NODECHECKLOOP_COUNT) {
			Iterable<WNode> nodeiterator = getNodesIterator();
			//
			for (WNode node : nodeiterator) {
				checkNode(node);
			}

			int timeout = 100 + (int) (Math.random() * 100 * this.nodes.size() * NODECHECKLOOP_COUNT);

			this.wait(getWaitTime(timeout));
		}
		log.info("Node check loop out. ThreadGroup active:"
				+ this.nodechecktg.activeCount());
	}

	private void checkNode(WNode node) {
		NodeStatus nodestatus = getNodeStatus(node);

		if (node.isConnected() && nodestatus.checkPing()) {
			sendPing(node);
		}
		//
		if (node.getID() != null && node.getID().equals(getMessenger().getID())) {
			log.info("Having myself as remote node. Removing.");
			node.close();
			removeNode(node);
		} else if (getNodeStatus(node).shouldDie() || node.isClosed()) {
			log.info("Removing node " + node);
			node.close();
			removeNode(node);
			nodestatuses.remove(node);
		}
	}

	private synchronized void removeNode(WNode node) {
		nodes.remove(node);
	}

	private long getWaitTime(long ntimeout) {
		long timeout = ntimeout;
		if (timeout < MINIMUM_TIMEOUT) {
			timeout = MINIMUM_TIMEOUT;
		}

		return timeout;
	}

	@Override
	public synchronized NodeStatus getNodeStatus(WNode node) {
		return nodestatuses.get(node);
	}

	private void sendPing(WNode node) {
		MMessage message = getMessenger().getMessage("ping");
		getMessenger().addResponseListener(message.getID(),
				new MessageResponseListenerImplementation());

		node.sendMessage(message);
		getNodeStatus(node).pingSent();
	}

	@Override
	public synchronized Iterable<WNode> getNodesIterator() {
		if (nodes != null) {
			if (nodes.isEmpty()) {
				addDefaultNodes();
			}
			return new LinkedList<WNode>(nodes);
		} else {
			return null;
		}
	}

	private synchronized void addDefaultNodes() {
		ConditionWaiter.wait(new ConditionWaiter.Condition() {
			@Override
			public boolean test() {
				String slist = p.get(WPreferences.SERVERLIST, "");
				return slist != null && slist.length() > 0;
			}
		}, 2000);

		addNodesInServerLists();
	}

	private void addNodesInServerLists() {
		String slist = p.get(WPreferences.SERVERLIST, "");
		log.info("got server list " + slist);

		if (slist == null || slist.length() == 0) {
			slist = createServerList();
		}

		StringTokenizer st = new StringTokenizer(slist, ",");
		while (st.hasMoreTokens()) {
			String server = st.nextToken();
			int indexOf = server.indexOf(':');
			if (indexOf > 0) {
				String host = server.substring(0, indexOf);
				int port = Integer.parseInt(server.substring(indexOf + 1));
				if ("localhost".equals(host) && tcplistener != null
						&& tcplistener.getPort() == port) {
					log.info("Not adding node @ " + server);
				} else {
					addNode(new MHost(host), port);
				}
			} else {
				log.info("invalid value " + server);
			}
		}
	}

	private String createServerList() {
		String slist;
		slist = "";
		log.info("Serverlist empty. Adding service domain with default port");
		String service = p.get(WPreferences.SERVICE_URL, "");
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
		return slist;
	}

	@Override
	public void addServerListener(ServerListener nodeListener) {
		synchronized (listeners) {
			listeners.add(nodeListener);
		}
	}

	@Override
	public boolean isRunning() {
		return !closed && nodes != null;
	}

	@Override
	public synchronized void close() {
		log.info("closing server");
		startClosing();
		//
		shutdown();
		nodes = null;

		log.info("closing done");
	}

	@Override
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

	private synchronized void shutdown() {
		log.info("shutting down");
		shutdownNodes();

		closeListener();
		//
		if (nodes != null) {
			notifyAll();
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
	public WNode getNode(MNodeID nid) {
		if (nid != null && nodes != null) {
			for (WNode node : nodes) {
				if (nid.equals(node.getID())) {
					return node;
				}
			}
		}

		log.info("node not found (" + nid + ")  in nodes " + nodes);

		return null;
	}

	public void nodeConnected(WNode node) {
		MMessage m = new MMessage("connectednode", getID());
		Set<MNodeID> exceptions = new HashSet<MNodeID>();
		exceptions.add(node.getID());
		getMessenger().broadcastMessage(m, null, exceptions);
	}

	@Override
	public MNodeID getID() {
		if (getMessenger() != null) {
			return getMessenger().getID();
		} else {
			return null;
		}
	}

	@Override
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

	@Override
	public void addDownload(Binary bs) {
		synchronized (downloads) {
			if (downloads.get(bs.getID()) == null) {
				log.info("adding download " + bs);
				downloads.put(bs.getID(),
						new Download(bs, this, getMessenger()));
			}
		}
	}

	@Override
	public void start() {
		startNetwork();
	}

	@Override
	public boolean canDownload() {
		return downloads.size() < p.getInteger(
				WPreferences.NETWORK_MAX_DOWNLOADS,
				WPreferences.NETWORK_MAX_DOWNLOADS_DEFAULT);
	}

	@Override
	public void clearMemory() {
		synchronized (listeners) {
			List<ServerListener> nsourcelisteners = new LinkedList<ServerListener>(
					listeners);
			for (ServerListener l : nsourcelisteners) {
				if (l.isDone()) {
					listeners.remove(l);
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

	@Override
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

	@Override
	public boolean isConnected() {
		if (nodes != null) {
			List<WNode> ns = new LinkedList<WNode>(this.nodes);
			for (WNode node : ns) {
				if (node.isConnected() && getNodeStatus(node) != null
						&& getNodeStatus(node).getReceivedMessages() > 0) {
					log.info("node connected " + node);
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void waitForConnection(int maxwaittime) {
		try {
			MTimedFlag timer = new MTimedFlag(maxwaittime);
			while (isRunning() && !isConnected()) {
				synchronized (this) {
					wait(100);
					if (timer.isTriggered()) {
						log.info("waitForConnection failed. Returning.");
						return;
					}
				}
			}
		} catch (InterruptedException e) {
			log.error(e);
		}
	}

	@Override
	public int getPort() {
		if (tcplistener != null) {
			return tcplistener.getPort();
		} else {
			return -1;
		}
	}

	@Override
	public WPreferences getPreferences() {
		return p;
	}

	@Override
	public WMessenger getMessenger() {
		return this.messenger;
	}

	private void loopChecker() {
		long lastmessagereceived = getMessenger().getLastMessageReceived();
		while (isRunning()) {
			long dt = System.currentTimeMillis() - lastmessagereceived;
			if (dt > REBOOT_DELAY) {
				reboot();
				lastmessagereceived = System.currentTimeMillis();
			} else {
				synchronized (this) {
					try {
						this.wait(getWaitTime(dt - REBOOT_DELAY));
					} catch (InterruptedException e) {
						log.error(e);
					}
				}
			}
		}
		log.info("Reboot checker out");
	}

	private final class MessageResponseListenerImplementation implements
			MessageResponseListener {
		private long sent = System.currentTimeMillis();
		private boolean done = false;

		@Override
		public void messageReceived(MMessage message) {
			log.info("PING response in " + (System.currentTimeMillis() - sent)
					+ " ms");
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
	}

}
