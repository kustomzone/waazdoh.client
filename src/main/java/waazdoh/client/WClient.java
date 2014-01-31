package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import waazdoh.client.rest.RestClient;
import waazdoh.cutils.MPreferences;
import waazdoh.cutils.MStringID;
import waazdoh.cutils.UserID;
import waazdoh.service.CMService;

public final class WClient {
	private CMService service;
	private MPreferences preferences;
	private MBinarySource source;
	private boolean running = true;
	//
	private Set<WClientListener> listeners = new HashSet<WClientListener>();
	private WBookmarks bookmarks;

	public WClient(MPreferences p, MBinarySource binarysource)
			throws MalformedURLException {
		this.preferences = p;
		this.source = binarysource;
		service = new RestClient(getServiceURL(), source);
	}

	public WClient(MPreferences p, MBinarySource binarysource,
			CMService nservice) {
		this.preferences = p;
		this.source = binarysource;
		this.service = nservice;
	}

	private String getServiceURL() {
		MPreferences p = getPreferences();
		return p.get(MPreferences.SERVICE_URL, "THIS_SHOULD_BE_SERVICE_URL");
	}

	public boolean isRunning() {
		return running && source != null && source.isRunning()
				&& service != null && service.isLoggedIn();
	}

	public WBookmarks getBookmarks() {
		return bookmarks;
	}

	public UserID getUserID() {
		return service.getUserID();
	}

	public MBinarySource getBinarySource() {
		return source;
	}

	public CMService getService() {
		return service;
	}

	public String getMemoryUsageInfo() {
		return source.getMemoryUsageInfo();
	}

	public String getInfoText() {
		return source.getInfoText();
	}

	public MPreferences getPreferences() {
		return preferences;
	}

	public boolean setUsernameAndSession(final String username, String session) {
		if (!service.isLoggedIn()) {
			if (service.setSession(username, session)) {
				source.setService(service);
				loggedIn();
				return true;
			} else {
				return false;
			}
		} else {
			return true;
		}
	}

	public String requestAppLogin(final String email, String appname, MStringID id) {
		if (service.getSessionID() == null) {
			String sessionid = service.requestAppLogin(email, appname, id);
			if (sessionid != null) {
				loggedIn();
			}
			//
			return sessionid;
		} else {
			return service.getSessionID();
		}
	}

	public void stop() {
		running = false;
		source.close();
	}

	private void loggedIn() {
		bookmarks = new WBookmarks(service);

		for (WClientListener clientListener : listeners) {
			clientListener.loggedIn();
		}
	}

}
