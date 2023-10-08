/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment;

import dev.yumi.gradle.licenser.util.Utils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;


/**
 * Represents the license comment reader and writer for C-style files.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class CStyleHeaderComment implements HeaderComment {
	/**
	 * The implementation instance of this header comment type.
	 */
	public static final CStyleHeaderComment INSTANCE = new CStyleHeaderComment();

	@Override
	public @NotNull Result readHeaderComment(@NotNull String source) {
		int start = 0, end;
		String found = null;

		for (end = 0; end < source.length(); end++) {
			char c = source.charAt(end);

			// Find comment start.
			if (c == '/') {
				if (Utils.matchCharAt(source, end + 1, '*')
						&& Utils.matchOtherCharAt(source, end + 2, '*')) {
					// Header!
					start = end;
					int j = end + 2;

					// Attempt to find the end of it.
					while (j < source.length()) {
						j = source.indexOf('*', j + 1);

						if (j == -1) {
							found = source.substring(end + 2);
							break;
						}

						if (j + 1 == source.length()) {
							found = source.substring(end + 2);
							end = j;
							break;
						}

						if (source.charAt(j + 1) == '/') {
							// The end!
							found = source.substring(end + 2, j - 1);
							end = j + 2;
							break;
						}
					}
				} else break;

				if (found != null) break;
			} else if (!Character.isWhitespace(c)) break;
		}

		String separator = this.extractLineSeparator(source);
		List<String> result = null;

		if (found != null) {
			String[] lines = found.split("\r?\n( ?\\* ?)?");
			lines[0] = lines[0].stripLeading();

			result = new ArrayList<>(List.of(lines));

			if (result.get(0).isBlank()) {
				result.remove(0);
			}
		}

		return new Result(start, end, result, separator);
	}

	@Override
	public @NotNull String writeHeaderComment(List<String> header, String separator) {
		var builder = new StringBuilder("/*").append(separator);

		for (var line : header) {
			if (line.isBlank()) {
				builder.append(" *").append(separator);
			} else {
				builder.append(" * ").append(line).append(separator);
			}
		}

		return builder + " */";
	}
}
