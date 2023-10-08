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
 * Represents a lenient year range variable type.
 * <p>
 * The default value of this variable type is the given creation year of the project or given file
 * depending on the {@linkplain dev.yumi.gradle.licenser.api.rule.LicenseYearSelectionMode the year selection mode}.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class YearLenientRangeVariableType implements VariableType<int[]> {
	/**
	 * The implementation instance of this variable type.
	 */
	public static final YearLenientRangeVariableType INSTANCE = new YearLenientRangeVariableType();

	@Override
	public @NotNull Optional<ParseResult<int[]>> parseVar(@NotNull String input, int start) {
		int firstYearEnd = Utils.findInteger(input, start);

		if (firstYearEnd == -1) {
			return Optional.empty();
		}

		int secondYearEnd = -1;
		int secondYear = -1;

		if (Utils.matchCharAt(input, firstYearEnd, '-')) {
			secondYearEnd = Utils.findInteger(input, firstYearEnd + 1);

			if (secondYearEnd != -1) {
				secondYear = Integer.parseInt(input.substring(firstYearEnd + 1, secondYearEnd));
			}
		}

		var years = new int[secondYear == -1 ? 1 : 2];
		years[0] = Integer.parseInt(input.substring(start, firstYearEnd));
		if (secondYear != -1) years[1] = secondYear;

		return Optional.of(new ParseResult<>(years, secondYear == -1 ? firstYearEnd : secondYearEnd));
	}

	@Override
	public @NotNull String getAsString(int @NotNull [] value) {
		String start = String.valueOf(value[0]);

		if (value.length > 1) {
			return start + '-' + value[1];
		} else {
			return start;
		}
	}

	@Override
	public int @NotNull [] getUpToDate(@NotNull HeaderFileContext context, int @Nullable [] old) {
		int modifiedYear = context.lastModifiedYear();

		if (old != null) {
			var years = old;

			if (years.length > 1) {
				if (years[1] < modifiedYear) {
					years[1] = modifiedYear;
				}
			} else if (years[0] < modifiedYear) {
				years = new int[]{years[0], modifiedYear};
			}

			return years;
		} else {
			int creationYear = context.creationYear();

			if (creationYear != modifiedYear) {
				return new int[]{creationYear, modifiedYear};
			} else {
				return new int[]{creationYear};
			}
		}
	}
}
