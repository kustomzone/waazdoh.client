package waazdoh.client.rest;

import java.net.MalformedURLException;

import waazdoh.client.BinarySource;
import waazdoh.client.WClient;
import waazdoh.client.storage.BeanStorage;
import waazdoh.common.WPreferences;
import waazdoh.common.client.RestServiceClient;

public class WRestClient extends WClient {

	public WRestClient(WPreferences p, BinarySource binarysource,
			BeanStorage beanstorage) throws MalformedURLException {
		super(p, binarysource, beanstorage, new RestServiceClient(p.get(
				WPreferences.SERVICE_URL, "THIS_SHOULD_BE_SERVICE_URL")));
	}
}
