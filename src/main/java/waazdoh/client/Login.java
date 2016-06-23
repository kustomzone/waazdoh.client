package waazdoh.client;

import retrofit.RetrofitError;
import waazdoh.client.storage.local.FileBeanStorage;
import waazdoh.client.utils.ConditionWaiter;
import waazdoh.common.WLogger;
import waazdoh.common.WPreferences;
import waazdoh.common.client.RestServiceClient;
import waazdoh.common.testing.StaticTestPreferences;
import waazdoh.common.util.PropertiesPreferences;
import waazdoh.common.vo.AppLoginVO;
import waazdoh.cp2p.P2PBinarySource;

public class Login {

	private WClient client;
	private PropertiesPreferences p;
	private WLogger log;

	public WClient login(String prefix, String username) {
		log = WLogger.getLogger(this);

		int tries = 0;
		while (tries++ < 10) {
			try {
				return tryLogin(prefix, username);
			} catch (RetrofitError e) {
				log.error(e);
				synchronized (this) {
					try {
						this.wait(10000);
					} catch (InterruptedException e1) {
						log.error(e1);
					}
				}
			}
		}
		
		return null;
	}

	private WClient tryLogin(String prefix, String username) {
		p = new PropertiesPreferences(prefix, username);

		FileBeanStorage beanstorage = new FileBeanStorage(p);
		P2PBinarySource binarysource = new P2PBinarySource(p, beanstorage, true);

		RestServiceClient serviceclient = new RestServiceClient(p.get(WPreferences.SERVICE_URL, "unknown_service"),
				beanstorage);

		client = new WClient(p, binarysource, beanstorage, serviceclient);

		String session = p.get("session", "");

		boolean setsession = client.setSession(session);
		if (setsession) {
			// DONE
			log.info("Login done with session " + session);
		} else {
			AppLoginVO applogin = client.requestAppLogin();
			String apploginid = applogin.getId();
			String url = applogin.getUrl();
			log.info("applogin url " + url + (url.charAt(url.length() - 1) == '/' ? "" : "/") + apploginid);

			ConditionWaiter.wait(() -> {
				AppLoginVO al = client.checkAppLogin(apploginid);
				return al.getSessionid() != null;
			}, 100000);

			applogin = client.checkAppLogin(applogin.getId());

			p.set("session", applogin.getSessionid());

		}

		return client;
	}

	public PropertiesPreferences getPreferences() {
		return p;
	}
}
