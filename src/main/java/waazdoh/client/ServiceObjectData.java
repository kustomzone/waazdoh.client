package waazdoh.client;

import waazdoh.cutils.xml.JBean;

public interface ServiceObjectData {

	boolean parseBean(JBean bean);

	JBean getBean();

}
