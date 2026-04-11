/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule;

import org.jetbrains.annotations.Contract;

/**
 * Represents the context of a file for which the license header is to be updated.
 *
 * @author LambdAurora
 * @version 4.0.0
 * @since 1.0.0
 */
public interface HeaderFileContext {
	/**
	 * {@return the name of the file to update}
	 */
	@Contract(pure = true)
	String fileName();

	/**
	 * {@return the creation year}
	 */
	int creationYear();

	/**
	 * {@return the last modified year}
	 */
	int lastModifiedYear();
}
