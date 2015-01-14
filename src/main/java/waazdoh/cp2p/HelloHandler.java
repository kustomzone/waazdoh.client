package waazdoh.cp2p;

import waazdoh.cp2p.messaging.MMessage;
import waazdoh.cp2p.messaging.MMessageFactory;
import waazdoh.cp2p.messaging.MMessageHandler;
import waazdoh.util.MLogger;

public class HelloHandler implements MMessageHandler {
	private MLogger log = MLogger.getLogger(this);

	@Override
	public MMessage handle(MMessage childb) {
		log.info("Hello received " + childb);
		// no response
		return null;
	}

	@Override
	public void setFactory(MMessageFactory factory) {
		// not needed
	}

}
