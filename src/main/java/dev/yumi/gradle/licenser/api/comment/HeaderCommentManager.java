/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment;

import org.gradle.api.file.FileTreeElement;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages the different header comment implementations for given file types.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class HeaderCommentManager {
	private final Map<PatternSet, HeaderComment> headers = new HashMap<>();

	public HeaderCommentManager() {
		this.register(new PatternSet()
						.include(
								"**/*.c",
								"**/*.cpp",
								"**/*.cxx",
								"**/*.h",
								"**/*.hpp",
								"**/*.hxx",
								"**/*.java",
								"**/*.kt",
								"**/*.kts",
								"**/*.scala"
						),
				CStyleHeaderComment.INSTANCE
		);
	}

	/**
	 * Registers a header comment implementation for a given file pattern.
	 *
	 * @param filePattern the file pattern to match files for which the given header comment implementation applies
	 * @param headerComment the header comment implementation
	 */
	public void register(@NotNull PatternSet filePattern, @NotNull HeaderComment headerComment) {
		this.headers.put(filePattern, headerComment);
	}

	/**
	 * Finds the header comment implementation to use for the given file.
	 *
	 * @param file the file
	 * @return the header comment implementation if a suitable one could be found, or {@code null} otherwise
	 */
	public @Nullable HeaderComment findHeaderComment(@NotNull FileTreeElement file) {
		for (var entry : this.headers.entrySet()) {
			if (entry.getKey().getAsSpec().isSatisfiedBy(file)) {
				return entry.getValue();
			}
		}

		return null;
	}
}
