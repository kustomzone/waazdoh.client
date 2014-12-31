package waazdoh.client;

import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import waazdoh.client.binaries.BinarySource;
import waazdoh.client.model.Bookmarks;
import waazdoh.client.model.UserID;
import waazdoh.client.model.WData;
import waazdoh.client.model.WResponse;
import waazdoh.client.model.WService;
import waazdoh.cp2p.network.MBeanStorage;
import waazdoh.service.rest.RestServiceClient;
import waazdoh.util.MPreferences;
import waazdoh.util.MStringID;

public final class WClient {
	private WService service;
	private MPreferences preferences;
	private BinarySource source;
	private boolean running = true;
	//
	private Set<WClientListener> listeners = new HashSet<WClientListener>();
	private Bookmarks bookmarks;
	private MBeanStorage beanstorage;

	public WClient(MPreferences p, BinarySource binarysource)
			throws MalformedURLException {
		this.preferences = p;
		this.source = binarysource;
		service = new RestServiceClient(getServiceURL(), source);
	}

	public WClient(MPreferences p, BinarySource binarysource, WService nservice) {
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

	public Bookmarks getBookmarks() {
		return bookmarks;
	}

	public UserID getUserID() {
		return service.getUserID();
	}

	public BinarySource getBinarySource() {
		return source;
	}

	public WService getService() {
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

	public boolean trySavedSession() {
		return setSession(getPreferences().get(
				MPreferences.PREFERENCES_SESSION, ""));
	}

	public boolean setSession(final String session) {
		if (!service.isLoggedIn()) {
			if (service.setSession(session)) {
				source.setService(service);
				getPreferences().set(MPreferences.PREFERENCES_SESSION, session);
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
		bookmarks = new Bookmarks(service);

		for (WClientListener clientListener : listeners) {
			clientListener.loggedIn();
		}
	}

	public WClientAppLogin requestAppLogin() {
		WData b = getService().requestAppLogin();
		return new WClientAppLogin(b);
	}

	public WClientAppLogin checkAppLogin(MStringID id) {
		WData b = getService().checkAppLogin(id);
		WClientAppLogin applogin = new WClientAppLogin(b);
		if (applogin.getSessionId() != null) {
			setSession(applogin.getSessionId());
		}
		return applogin;
	}

	public String readStorageArea(String string) {
		return getService().readStorageArea(string);
	}

	public List<MStringID> search(String searchitem, int index, int count) {
		WResponse bresult = getService().search(searchitem, index, count);
		List<MStringID> idlist = bresult.getIDList();
		return idlist;
	}

	public MBeanStorage getBeanStorage() {
		return this.beanstorage;
	}
}
