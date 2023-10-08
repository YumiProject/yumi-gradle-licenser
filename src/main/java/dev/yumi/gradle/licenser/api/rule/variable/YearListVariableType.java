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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Represents a list of years variable type.
 * <p>
 * The default value of this variable type is the given creation year of the project or given file
 * depending on the {@linkplain dev.yumi.gradle.licenser.api.rule.LicenseYearSelectionMode the year selection mode}.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class YearListVariableType implements VariableType<int[]> {
	/**
	 * The implementation instance of this variable type.
	 */
	public static final YearListVariableType INSTANCE = new YearListVariableType();

	@Override
	public @NotNull Optional<ParseResult<int[]>> parseVar(@NotNull String input, int start) {
		IntStream.Builder years = IntStream.builder();
		boolean foundNext, foundOne = false;
		int totalEnd = -1;

		do {
			int numberEnd = Utils.findInteger(input, start);

			if (numberEnd == -1) {
				break;
			}

			totalEnd = numberEnd;
			foundOne = true;
			years.add(Integer.parseInt(input.substring(start, numberEnd)));

			if (Utils.matchCharAt(input, numberEnd, ',')
					&& Utils.matchCharAt(input, numberEnd + 1, ' ')) {
				start = numberEnd + 2;
				foundNext = true;
			} else {
				foundNext = false;
			}
		} while (foundNext);

		if (!foundOne) {
			return Optional.empty();
		} else {
			return Optional.of(new ParseResult<>(years.build().sorted().toArray(), totalEnd));
		}
	}

	@Override
	public @NotNull String getAsString(int @NotNull [] value) {
		return Arrays.stream(value).mapToObj(String::valueOf).collect(Collectors.joining(", "));
	}

	@Override
	public int @NotNull [] getUpToDate(@NotNull HeaderFileContext context, int @Nullable [] old) {
		int lastModified = context.lastModifiedYear();

		if (old != null) {
			int[] years = old;
			int lastKnown = years[years.length - 1];

			if (lastKnown < lastModified) {
				IntStream yearsStream = IntStream.of(years);
				IntStream newYears = IntStream.rangeClosed(lastKnown + 1, lastModified);
				years = IntStream.concat(yearsStream, newYears)
						.distinct()
						.sorted()
						.toArray();
			}

			return years;
		} else {
			return IntStream.rangeClosed(context.creationYear(), lastModified).toArray();
		}
	}
}
