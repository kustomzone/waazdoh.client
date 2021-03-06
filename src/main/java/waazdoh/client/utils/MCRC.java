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
package waazdoh.client.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public final class MCRC {
	private static final int ERROR_VALUE = -1;
	private long value;
	private CRC32 crc32;

	@Override
	public String toString() {
		return "MCRC" + value + "]";
	}

	public MCRC() {
		value = ERROR_VALUE;
		crc32 = new CRC32();
	}

	public MCRC(long attributeLong) {
		this.value = attributeLong;
	}

	public MCRC(InputStream inputStream) throws IOException {
		crc32 = new CRC32();
		while (true) {
			int b = inputStream.read();
			if (b < 0) {
				break;
			}
			update((byte) b);
		}
	}

	public void update(Byte b) {
		crc32.update(b);
		value = crc32.getValue();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		} else {
			if (obj instanceof MCRC) {
				MCRC crc = (MCRC) obj;
				if (crc.isError()) {
					return false;
				}
				if (crc.value > 0 && crc.value >= 0 && value >= 0) {
					return crc.value == value;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
	}

	@Override
	public int hashCode() {
		return (int) value;
	}

	public long getValue() {
		return value;
	}

	public static MCRC error() {
		return new MCRC(ERROR_VALUE);
	}

	public boolean isError() {
		return value == ERROR_VALUE;
	}
}
