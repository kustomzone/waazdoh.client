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
package waazdoh.cutils;

public final class MTimedFlag {
	private long lastreset;
	private int delaytime;

	public MTimedFlag(int readytimer) {
		delaytime = readytimer;
		reset();
	}

	public void reset() {
		this.lastreset = System.currentTimeMillis();
	}

	public boolean isTriggered() {
		return System.currentTimeMillis() - lastreset > delaytime;
	}

	public void trigger() {
		lastreset = 0;
		synchronized (this) {
			notifyAll();
		}
	}

	@Override
	public String toString() {
		return "Trigger[" + isTriggered() + "]["
				+ (System.currentTimeMillis() - lastreset) + "]";
	}

	public void waitTimer() {
		while (!isTriggered()) {
			synchronized (this) {
				try {
					wait(delaytime / 10);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
