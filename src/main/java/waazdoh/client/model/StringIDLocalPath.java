package waazdoh.client.model;

import java.io.File;

public class StringIDLocalPath {

	private String localpath;
	private String id;

	public StringIDLocalPath(String localpath, MBinaryID mBinaryID) {
		this.localpath = localpath.toString();
		this.id = mBinaryID.toString();
	}

	public StringIDLocalPath(String path, String id2) {
		this.localpath = path;
		this.id = id2;
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
