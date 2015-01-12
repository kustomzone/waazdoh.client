package waazdoh.client;

import waazdoh.client.model.WData;
import waazdoh.util.MStringID;

public interface BeanStorage {

	WData getBean(MStringID id);

	void addBean(MStringID id, WData b);

	Iterable<MStringID> getLocalSetIDs(String search);

}
