/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.util;

import dev.yumi.gradle.licenser.YumiLicenserGradleExtension;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Predicate;

/**
 * Provides various utilities.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class Utils {
	private Utils() {
		throw new UnsupportedOperationException("Utils only contains static definitions.");
	}

	/**
	 * Matches a character in a string at a given index which could be out of bounds.
	 *
	 * @param source the source string
	 * @param index the index, which may be out of bounds
	 * @param expected the expected character at the given index
	 * @return {@code true} if the character matched at the given index, or {@code false} otherwise
	 */
	public static boolean matchCharAt(String source, int index, char expected) {
		return index >= 0
				&& index < source.length()
				&& source.charAt(index) == expected;
	}

	/**
	 * Matches the lack of a character in a string at a given index which could be out of bounds.
	 *
	 * @param source the source string
	 * @param index the index, which may be out of bounds
	 * @param unexpected the unexpected character at the given index
	 * @return {@code true} if the character hasn't matched at the given index, or {@code false} otherwise
	 */
	public static boolean matchOtherCharAt(String source, int index, char unexpected) {
		return index >= 0
				&& index < source.length()
				&& source.charAt(index) != unexpected;
	}

	/**
	 * Attempts to read an integer from the given string at the given index.
	 *
	 * @param source the string to read from
	 * @param start the index where the integer should start
	 * @return the end index of the integer if an integer has been found, or {@code -1} otherwise
	 */
	public static int findInteger(@NotNull String source, @Range(from = 0, to = Integer.MAX_VALUE) int start) {
		for (int i = start; i < source.length(); i++) {
			char c = source.charAt(i);

			if (!(c >= '0' && c <= '9')) {
				return i == start ? -1 : i;
			}
		}

		return -1;
	}

	/**
	 * Trims empty lines in the given line list.
	 *
	 * @param list the line list
	 * @param emptyPredicate the predicate which returns {@code true} if the given line is empty, or {@code false} otherwise
	 * @param <T> the type of the lines
	 */
	public static <T> void trimLines(@NotNull List<T> list, @NotNull Predicate<T> emptyPredicate) {
		var it = list.iterator();
		while (it.hasNext()) {
			if (emptyPredicate.test(it.next())) {
				it.remove();
			} else {
				break;
			}
		}

		int toRemoveAtEnd = list.size();
		for (int i = toRemoveAtEnd - 1; i >= 0; i--) {
			if (!emptyPredicate.test(list.get(i))) {
				toRemoveAtEnd = i + 1;
				break;
			}
		}

		if (toRemoveAtEnd != list.size()) {
			list.subList(toRemoveAtEnd, list.size()).clear();
		}
	}

	/**
	 * Gets the backup path for a given source file in the specified project.
	 *
	 * @param project the project
	 * @param rootPath the root path
	 * @param path the path of the file to back up
	 * @return the backup path, or {@code null} if something went wrong
	 * @throws IOException if the backup directories couldn't be created
	 */
	public static @Nullable Path getBackupPath(Project project, Path rootPath, Path path) throws IOException {
		Path backupDir = project.getLayout().getBuildDirectory().getAsFile().get().toPath().resolve("yumi/licenser");

		Files.createDirectories(backupDir);

		var pathAsString = path.toAbsolutePath().toString();
		var rootPathAsString = rootPath.toString();

		if (pathAsString.startsWith(rootPathAsString)) {
			return backupDir.resolve(Paths.get(pathAsString.substring(rootPathAsString.length() + 1)))
					.normalize();
		}

		return null;
	}

	/**
	 * Gets the project creation year for the given project.
	 *
	 * @param project the project
	 * @return the creation year
	 */
	public static int getProjectCreationYear(Project project) {
		if (project.getRootProject() != project) {
			var ext = project.getRootProject().getExtensions().findByType(YumiLicenserGradleExtension.class);

			if (ext != null) {
				if (ext.getProjectCreationYear().isPresent()) {
					return ext.getProjectCreationYear().get();
				}
			}

			return getProjectCreationYear(project.getRootProject());
		}

		try {
			Instant instant = Files.readAttributes(project.getProjectDir().toPath(), BasicFileAttributes.class).creationTime().toInstant();
			LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
			int localCreationYear = localDate.getYear();

			return localCreationYear;
		} catch (IOException e) {
			throw new GradleException("Could not read creation year of the root project directory.");
		}
	}
}
