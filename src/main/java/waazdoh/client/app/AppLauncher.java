package waazdoh.client.app;

import waazdoh.client.Login;
import waazdoh.client.WClient;
import waazdoh.client.model.BinaryID;
import waazdoh.common.MStringID;
import waazdoh.common.WLogger;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageHandler;

public class AppLauncher {
	private WClient client;
	private WLogger log = WLogger.getLogger(this);

	private void start() {
		Login login = new Login();
		String username = "downloader";
		client = login.login("downloadeverything", username);
		client.getBinarySource().addMessageHandler(WMessenger.MESSAGENAME_PUBLISHED, new PublishedHandler());

		new Thread(() -> {
			try {
				run();
			} catch (InterruptedException e) {
				log.error(e);
			}
		}).start();
	}

	private synchronized void run() throws InterruptedException {
		log.info("running with " + client);

		while (client.isRunning()) {
			log.info("still running with " + client);
			wait(10000);
		}

		log.info("Done " + client);
	}

	public static void main(String[] args) {
		AppLauncher d = new AppLauncher();
		d.start();
	}

	private class PublishedHandler implements MMessageHandler {
		private WMessenger setMessenger;

		@Override
		public MMessage handle(MMessage m) {
			MStringID binaryid = m.getIDAttribute("binaryid");
			client.getBinarySource().getOrDownload(new BinaryID(binaryid));
			return setMessenger.newResponseMessage(m, "ok");
		}

		@Override
		public void setMessenger(WMessenger factory) {
			this.setMessenger = factory;
		}
	}
}
