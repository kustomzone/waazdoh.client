package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

import waazdoh.client.model.JBean;
import waazdoh.client.model.UserID;
import waazdoh.client.model.WBookmarks;
import waazdoh.client.rest.RestServiceClient;
import waazdoh.service.CMService;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

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
		service = new RestServiceClient(getServiceURL(), source);
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

	public boolean setSession(final String session) {
		if (!service.isLoggedIn()) {
			if (service.setSession(session)) {
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

	public void stop() {
		running = false;
		source.close();
	}

	public void addListener(WClientListener listener) {
		listeners.add(listener);
	}

	private void loggedIn() {
		bookmarks = new WBookmarks(service);

		for (WClientListener clientListener : listeners) {
			clientListener.loggedIn();
		}
	}

	public WClientAppLogin requestAppLogin() {
		JBean b = getService().requestAppLogin();
		return new WClientAppLogin(b);
	}

	public WClientAppLogin checkAppLogin(MStringID id) {
		JBean b = getService().checkAppLogin(id);
		WClientAppLogin applogin = new WClientAppLogin(b);
		if (applogin.getSessionId() != null) {
			setSession(applogin.getSessionId());
		}
		return applogin;
	}
}
