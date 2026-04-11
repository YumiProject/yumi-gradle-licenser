/*
 * Copyright 2026 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.impl;

import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;

import java.util.function.IntSupplier;

final class HeaderFileContextImpl implements HeaderFileContext {
	private final String fileName;
	private final IntSupplier creationYear;
	private final IntSupplier lastModifiedYear;

	HeaderFileContextImpl(String fileName, IntSupplier creationYear, IntSupplier lastModifiedYear) {
		this.fileName = fileName;
		this.creationYear = creationYear;
		this.lastModifiedYear = lastModifiedYear;
	}

	@Override
	public String fileName() {
		return this.fileName;
	}

	@Override
	public int creationYear() {
		return this.creationYear.getAsInt();
	}

	@Override
	public int lastModifiedYear() {
		return this.lastModifiedYear.getAsInt();
	}
}
