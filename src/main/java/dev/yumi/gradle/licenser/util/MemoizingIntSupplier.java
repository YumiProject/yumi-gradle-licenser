/*
 * Copyright 2026 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.util;

import java.util.function.IntSupplier;

public final class MemoizingIntSupplier implements IntSupplier {
	private volatile boolean initialized = false;
	private final Object lock = new Object();
	private Integer value;
	private final IntSupplier delegate;

	public MemoizingIntSupplier(IntSupplier delegate) {
		this.delegate = delegate;
	}

	@Override
	public int getAsInt() {
		if (!this.initialized) {
			synchronized (this.lock) {
				if (!this.initialized) {
					int value = this.delegate.getAsInt();
					this.value = value;
					this.initialized = true;
					return value;
				}
			}
		}

		return this.value;
	}
}
