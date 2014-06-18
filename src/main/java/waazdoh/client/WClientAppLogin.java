package waazdoh.client;

import waazdoh.util.MStringID;
import waazdoh.util.xml.JBean;

public class WClientAppLogin {

	private MStringID id;
	private String sessionid;
	private String url;

	public WClientAppLogin(JBean b) {
		this.id = b.getIDValue("id");
		this.sessionid = b.getValue("sessionid");
		this.url = b.getValue("url");
	}

	public String getSessionId() {
		return sessionid;
	}

	public String getURL() {
		return url;
	}

	public MStringID getId() {
		return id;
	}
}
