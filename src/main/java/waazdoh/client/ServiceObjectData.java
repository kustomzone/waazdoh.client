package waazdoh.client;

import waazdoh.util.xml.JBean;

public interface ServiceObjectData {

	boolean parseBean(JBean bean);

	JBean getBean();

}
