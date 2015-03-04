package waazdoh.cp2p;

import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageFactory;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.util.MLogger;

public class HelloHandler implements MMessageHandler {
	public static final String HELLO = "hello";
	public static final String HOLA = "hola";

	private MLogger log = MLogger.getLogger(this);
	private MMessageFactory factory;

	@Override
	public MMessage handle(MMessage childb) {
		log.info("Hello received " + childb);
		if (HelloHandler.HELLO.equals(childb.getName())) {
			return factory.newResponseMessage(childb, HelloHandler.HOLA);
		} else {
			return null;
		}
	}

	@Override
	public void setFactory(MMessageFactory factory) {
		this.factory = factory;
	}

}
