package waazdoh.cp2p;

import waazdoh.common.WLogger;
import waazdoh.cp2p.common.WMessenger;
import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageHandler;

public class HelloHandler implements MMessageHandler {
	public static final String HELLO = "hello";
	public static final String HOLA = "hola";

	private WLogger log = WLogger.getLogger(this);
	private WMessenger messenger;

	@Override
	public MMessage handle(MMessage childb) {
		log.info("Hello received " + childb);
		if (HelloHandler.HELLO.equals(childb.getName())) {
			return messenger.newResponseMessage(childb, HelloHandler.HOLA);
		} else {
			return null;
		}
	}

	@Override
	public void setMessenger(WMessenger nmessenger) {
		this.messenger = nmessenger;
	}
}
