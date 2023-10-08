/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule.variable;

import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

/**
 * Represents a type of variable.
 *
 * @param <D> the data type used by the variable type
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public interface VariableType<D> {
	/**
	 * The name of the default variable that represents the creation year, whose value is {@value}.
	 */
	String CREATION_YEAR_VAR_NAME = "CREATION_YEAR";
	/**
	 * The name of the default variable that represents the file name, whose value is {@value}.
	 */
	String FILE_NAME_VAR_NAME = "FILE_NAME";

	/**
	 * The known variable types.
	 */
	Map<String, VariableType<?>> TYPES = Map.of(
			CREATION_YEAR_VAR_NAME, CreationYearVariableType.INSTANCE,
			FILE_NAME_VAR_NAME, FileNameVariableType.INSTANCE,
			"YEAR_LENIENT_RANGE", YearLenientRangeVariableType.INSTANCE,
			"YEAR_LIST", YearListVariableType.INSTANCE
	);
	/**
	 * The default variables with their type.
	 */
	Map<String, VariableType<?>> DEFAULT_VARIABLES = Map.of(
			CREATION_YEAR_VAR_NAME, CreationYearVariableType.INSTANCE,
			FILE_NAME_VAR_NAME, FileNameVariableType.INSTANCE
	);

	/**
	 * Parses the given string input for this variable type.
	 *
	 * @param input the string input
	 * @param start the start index of the variable in the given string input
	 * @return the parsed data if successful, or {@linkplain Optional#empty() empty} otherwise
	 */
	@Contract(pure = true)
	@NotNull Optional<ParseResult<D>> parseVar(@NotNull String input, int start);

	/**
	 * Returns the string representation of the given value whose type is this variable type.
	 *
	 * @param value the value
	 * @return the string representation
	 */
	@Contract(pure = true)
	@NotNull String getAsString(@NotNull D value);

	/**
	 * Returns the up-to-date value for this variable type given the file context and the old value.
	 *
	 * @param context the context of which file is updated
	 * @param old the previous known value for this variable type, or {@code null} if unknown
	 * @return the up-to-date value for this variable type
	 */
	@NotNull D getUpToDate(@NotNull HeaderFileContext context, @Nullable D old);

	/**
	 * Represents the result of a parsed variable.
	 *
	 * @param data the data parsed
	 * @param end the end index of the variable value in the source string
	 * @param <D> the type of data
	 */
	record ParseResult<D>(D data, int end) {}
}
