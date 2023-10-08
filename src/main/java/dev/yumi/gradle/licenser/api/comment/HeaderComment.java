/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Represents the header comment reader and writer for a language.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public interface HeaderComment {
	/**
	 * Attempts to find the header comment and extract it from the given source.
	 *
	 * @param source the source
	 * @return the read result
	 */
	@Contract(pure = true)
	@NotNull Result readHeaderComment(@NotNull String source);

	/**
	 * Extracts the line separator used for the given source string.
	 *
	 * @param source the source string
	 * @return the line separator
	 */
	@Contract(pure = true)
	default @NotNull String extractLineSeparator(@NotNull String source) {
		int firstNewLineIndex = source.indexOf('\n');

		if (firstNewLineIndex == -1) {
			return System.lineSeparator();
		} else {
			if (firstNewLineIndex != 0 && source.charAt(firstNewLineIndex - 1) == '\r')
				return "\r\n";
			else
				return "\n";
		}
	}

	/**
	 * Formats the given header to the proper comment format.
	 *
	 * @param header the header comment to format
	 * @param separator the line separator to use
	 * @return the formatted header comment
	 */
	@NotNull String writeHeaderComment(List<String> header, String separator);

	/**
	 * Represents a header comment parsing result.
	 *
	 * @param start the start index of the parsed header comment
	 * @param end the end index of the parsed header comment
	 * @param existing the parsed header comment lines if found, or {@code null} otherwise
	 * @param separator the line separator used in the parsed file
	 */
	record Result(int start, int end, @Nullable List<String> existing, @NotNull String separator) {
	}
}
