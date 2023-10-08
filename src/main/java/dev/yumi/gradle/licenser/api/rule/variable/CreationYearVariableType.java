/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule.variable;

import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;
import dev.yumi.gradle.licenser.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Represents a creation year variable type.
 * <p>
 * The default value of this variable type is the given creation year of the project or given file
 * depending on the {@linkplain dev.yumi.gradle.licenser.api.rule.LicenseYearSelectionMode the year selection mode}.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class CreationYearVariableType implements VariableType<Integer> {
	/**
	 * The implementation instance of this variable type.
	 */
	public static final CreationYearVariableType INSTANCE = new CreationYearVariableType();

	@Override
	public @NotNull Optional<ParseResult<Integer>> parseVar(@NotNull String input, int start) {
		int numberEnd = Utils.findInteger(input, start);

		if (numberEnd == -1) {
			return Optional.empty();
		} else {
			return Optional.of(new ParseResult<>(Integer.parseInt(input.substring(start, numberEnd)), numberEnd));
		}
	}

	@Override
	public @NotNull String getAsString(@NotNull Integer value) {
		return value.toString();
	}

	@Override
	public @NotNull Integer getUpToDate(@NotNull HeaderFileContext context, @Nullable Integer old) {
		if (old != null) {
			return old;
		} else {
			return context.creationYear();
		}
	}
}
