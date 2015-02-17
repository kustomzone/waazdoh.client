package waazdoh.cp2p;

import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageFactory;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.util.MLogger;

public class HelloHandler implements MMessageHandler {
	private MLogger log = MLogger.getLogger(this);
	private MMessageFactory factory;

	@Override
	public MMessage handle(MMessage childb) {
		log.info("Hello received " + childb);
		return factory.newResponseMessage(childb, "hola");
	}

	@Override
	public void setFactory(MMessageFactory factory) {
		this.factory = factory;
	}

}
