/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule.variable;

import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a file name variable type.
 * <p>
 * This variable type has only one valid value for a given file which is its name.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class FileNameVariableType implements VariableType<String> {
	/**
	 * The implementation instance of this variable type.
	 */
	public static final FileNameVariableType INSTANCE = new FileNameVariableType();

	@Override
	public @NotNull Optional<ParseResult<String>> parseVar(@NotNull String input, int start) {
		int i;

		for (i = start; i < input.length(); i++) {
			char c = input.charAt(i);

			if (c == ' ' || c == '\t') {
				return i == start ? Optional.empty() : Optional.of(new ParseResult<>(input.substring(start, i), i));
			}
		}

		if (i != start) {
			return Optional.of(new ParseResult<>(input.substring(start, i), i));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public @NotNull String getAsString(@NotNull String value) {
		return value;
	}

	@Override
	public @NotNull String getUpToDate(@NotNull HeaderFileContext context, @Nullable String old) {
		return context.fileName();
	}
}
