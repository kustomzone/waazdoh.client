/*******************************************************************************
 * Copyright (c) 2013 Juuso Vilmunen.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Juuso Vilmunen - initial API and implementation
 ******************************************************************************/
package waazdoh.client.storage.local;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

import waazdoh.client.model.StringIDLocalPath;
import waazdoh.common.BeanStorage;
import waazdoh.common.MStringID;
import waazdoh.common.WLogger;
import waazdoh.common.WObject;
import waazdoh.common.WPreferences;

public final class FileBeanStorage implements BeanStorage {
	private WLogger log = WLogger.getLogger(this);
	private String path;

	public FileBeanStorage(WPreferences preferences) {
		this.path = preferences.get(WPreferences.LOCAL_PATH,
				WPreferences.LOCAL_PATH_DEFAULT) + File.separator + "beans";
		File file = new File(path);
		file.mkdirs();
	}

	public WObject getBean(final MStringID id) {
		try {
			File f = getFile(id);
			if (f.exists()) {
				String content = new String(Files.readAllBytes(Paths.get(f
						.getAbsolutePath())));
				WObject o = new WObject();
				o.parse(content);
				return o;
			} else {
				return null;
			}
		} catch (IOException e) {
			log.error(e);
			return null;
		}
	}

	private File getFile(final MStringID id) {
		File f = new File(getFilePath(id));
		return f;
	}

	private String getFilePath(final MStringID id) {
		String filepath = new StringIDLocalPath(this.path, id).getPath();
		File fpath = new File(filepath);
		if (!fpath.isDirectory()) {
			fpath.mkdirs();
		}
		//
		return filepath + id + ".yml";
	}

	@Override
	public void remove(MStringID id) {
		File f = getFile(id);
		if (f.exists()) {
			f.delete();
		}
	}

	public void addObject(final MStringID id, WObject response) {
		try {
			File f = getFile(id);
			FileWriter fw;
			fw = new FileWriter(f);
			BufferedWriter bw = new BufferedWriter(fw);
			bw.write(response.toText());
			bw.close();
		} catch (IOException e) {
			log.error(e);
		}
	}

	@Override
	public Iterable<MStringID> getLocalSetIDs(final String search) {
		final Iterator<File> fileiterator = getFileItarator(search);
		Iterable<MStringID> ids = getStringIDIterator(fileiterator);
		return ids;
	}

	private Iterable<MStringID> getStringIDIterator(
			final Iterator<File> fileiterator) {
		return new Iterable<MStringID>() {

			@Override
			public Iterator<MStringID> iterator() {
				return new Iterator<MStringID>() {
					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean hasNext() {
						return fileiterator.hasNext();
					}

					@Override
					public MStringID next() {
						File f = fileiterator.next();
						String name = f.getName();
						return new MStringID(name.replace(".yml", ""));
					}
				};
			}
		};
	}

	private Iterator<File> getFileItarator(final String search) {
		return FileUtils.iterateFiles(new File(path), new IOFileFilter() {

			@Override
			public boolean accept(File f) {
				String path = f.getAbsolutePath().replace(File.separator, "");
				log.info("fileiterator accept " + f);
				return path.indexOf(search) >= 0;
			}

			@Override
			public boolean accept(File arg0, String arg1) {
				log.info("not accepting " + arg0);

				return false;
			}

		}, new IOFileFilter() {

			@Override
			public boolean accept(File arg0, String arg1) {
				return true;
			}

			@Override
			public boolean accept(File arg0) {
				return true;
			}
		});
	}

}
