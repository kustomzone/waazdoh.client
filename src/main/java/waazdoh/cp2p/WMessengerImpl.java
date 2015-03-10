package waazdoh.cp2p;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import waazdoh.client.model.WData;
import waazdoh.cp2p.common.MHost;
import waazdoh.cp2p.common.MNodeID;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.cp2p.messaging.MessageID;
import waazdoh.cp2p.messaging.MessageResponseListener;
import waazdoh.cp2p.network.WMessenger;
import waazdoh.cp2p.network.WNode;
import waazdoh.util.MLogger;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public class WMessengerImpl implements WMessenger {
	private static final int MAX_SENTCOUNT = 2;

	private MNodeID networkid;
	private Map<String, MMessageHandler> handlers = new HashMap<String, MMessageHandler>();
	private Map<MessageID, MessageResponseListener> responselisteners = new HashMap<MessageID, MessageResponseListener>();
	private long lastmessagereceived;
	private long bytecountstart = System.currentTimeMillis();
	private int inputbytecount;
	private long outputbytecount;

	private MPreferences preferences;
	private MLogger log = MLogger.getLogger(this);
	private boolean closed;

	private P2PServer server;

	public WMessengerImpl(P2PServer server, MPreferences p) {
		this.networkid = new MNodeID(new MStringID());
		this.preferences = p;
		this.server = server;

		initHandlers();
		startNetwork();
	}

	@Override
	public MNodeID getID() {
		return this.networkid;
	}

	public void startNetwork() {
		if (!isClosed()) {
			log.info("Starting network");
			//
			lastmessagereceived = System.currentTimeMillis();
			if (networkid == null) {
				networkid = new MNodeID(new MStringID());
			}
		} else {
			log.info("Not starting the network. Closed.");
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public void close() {
		log.info("Closing");
		closed = true;
	}

	public WNode getNode(MNodeID id) {
		return server.getNode(id);
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

	@Override
	public long getLastMessageReceived() {
		return this.lastmessagereceived;
	}

	@Override
	public MMessage newResponseMessage(MMessage childb, String string) {
		MMessage m = getMessage(string);
		m.addAttribute("responseto", childb.getAttribute("messageid"));
		m.addIDAttribute("to", childb.getSentBy());
		m.addAttribute("date", "" + new Date());
		return m;
	}

	@Override
	public MMessage getMessage(final String string) {
		MMessage ret = new MMessage(string, networkid);
		return ret;
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
						sentbynode = this.server.addNode(sentby);
					}

					server.getNodeStatus(sentbynode).messageReceived(
							message.getName());
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

	public void broadcastMessage(MMessage notification) {
		broadcastMessage(notification, null);
	}

	@Override
	public void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener) {
		broadcastMessage(notification, messageResponseListener, null);
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

	private MMessageHandler getHandler(final String name) {
		return handlers.get(name);
	}

	private void initHandlers() {
		handlers.put("ping", new PingHandler());
		HelloHandler helloh = new HelloHandler();
		handlers.put(HelloHandler.HELLO, helloh);
		handlers.put(HelloHandler.HOLA, helloh);
		//
		for (MMessageHandler handler : handlers.values()) {
			handler.setMessenger(this);
		}
	}

	@Override
	public void addMessageHandler(String name, MMessageHandler h) {
		handlers.put(name, h);
		h.setMessenger(this);
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
			server.getNodeStatus(node).warning();
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

	private void handleNodeInfo(WData nodeinfo) {
		List<WData> nodeinfos = nodeinfo.getChildren();
		for (WData inode : nodeinfos) {
			MNodeID nodeinfoid = new MNodeID(inode.getName());
			if (getNode(nodeinfoid) == null) {
				MHost host = new MHost(inode.getValue("host"));
				int port = inode.getIntValue("port");
				server.addNode(host, port);
			}
		}
	}

	private MessageResponseListener getResponseListener(MessageID id) {
		synchronized (responselisteners) {
			return responselisteners.get(id);
		}
	}

	private void removeResponseListener(MessageID id) {
		synchronized (responselisteners) {
			responselisteners.remove(id);
		}

	}

	@Override
	public String getInfoText() {
		StringBuilder sb = new StringBuilder();

		long dtime = System.currentTimeMillis() - bytecountstart;
		if (dtime > 0) {
			sb.append(" I:" + (inputbytecount * 1000 / dtime));
			sb.append(" O:" + (outputbytecount * 1000 / dtime));
		}
		//
		if (dtime > 10 * 1000) {
			bytecountstart = System.currentTimeMillis();
			inputbytecount = 0;
			outputbytecount = 0;
		}

		return sb.toString();
	}

	public void broadcastMessage(MMessage notification,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions) {
		if (!isClosed()) {
			if (notification.getSentCount() <= MAX_SENTCOUNT) {
				sendToNodes(notification, messageResponseListener, exceptions);
			} else {
				log.info("not sending m" + "essage " + notification
						+ " due sentcount " + notification.getSentCount());
			}
		} else {
			log.info("CLOSED. Not broadcasting.");
		}
	}

	private void sendToNodes(MMessage notification,
			MessageResponseListener messageResponseListener,
			Set<MNodeID> exceptions) {
		notification.addAttribute("sentcount", notification.getSentCount() + 1);

		Iterable<WNode> nodes = server.getNodesIterator();
		for (WNode node : nodes) {
			if (node.isConnected()) {
				if (exceptions == null || !exceptions.contains(node.getID())) {
					node.sendMessage(notification);
					addResponseListener(notification.getID(),
							messageResponseListener);
				} else {
					log.debug("Not broadcasting to " + node
							+ " because node is in exceptions list");
				}
			} else {
				log.info("not broadcasting to " + node
						+ " node because it's not yet connected");
			}
		}
	}

}
