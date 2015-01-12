package waazdoh.client.rest;

import java.net.MalformedURLException;

import waazdoh.client.BinarySource;
import waazdoh.client.WClient;
import waazdoh.client.service.rest.RestService;
import waazdoh.client.storage.BeanStorage;
import waazdoh.util.MPreferences;

public class WRestClient extends WClient {

	public WRestClient(MPreferences p, BinarySource binarysource,
			BeanStorage beanstorage) throws MalformedURLException {
		super(p, binarysource, beanstorage, new RestService(p.get(
				MPreferences.SERVICE_URL, "THIS_SHOULD_BE_SERVICE_URL"),
				beanstorage));
	}
}
