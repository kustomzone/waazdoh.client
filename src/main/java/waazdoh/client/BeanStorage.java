package waazdoh.client;

import waazdoh.client.model.WData;

public interface BeanStorage {

	WData getBean(String string);

	void addBean(String string, WData b);

}
