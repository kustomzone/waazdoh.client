package waazdoh.client.model;

import java.io.File;

import waazdoh.util.MStringID;

public class StringIDLocalPath {

	private String localpath;
	private MStringID id;

	public StringIDLocalPath(String path, MStringID nid) {
		this.localpath = path;
		this.id = nid;
	}

	public String getPath() {
		String spath = "" + localpath;
		String sid = id.toString();
		// creating a directory tree
		// first four characters are likely a year.
		int index = 0;
		int indexjump = 4;
		while (index <= 4) {
			spath += File.separatorChar;
			spath += sid.substring(index, index + indexjump);
			index += indexjump;
			if (indexjump > 2) {
				indexjump = 2;
			}
		}

		spath += File.separatorChar;
		return spath;
	}
}
