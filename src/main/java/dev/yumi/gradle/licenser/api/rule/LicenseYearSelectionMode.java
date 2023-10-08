/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule;

import dev.yumi.gradle.licenser.YumiLicenserGradleExtension;
import dev.yumi.gradle.licenser.util.GitUtils;
import org.gradle.api.Project;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Represents the mode in which the year should be fetched.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public enum LicenseYearSelectionMode {
	/**
	 * The license year is project-wide, a change in any file of the project will update every file.
	 */
	PROJECT((project, path) -> project.getRootProject().getProjectDir().toPath()),
	/**
	 * Each file has its own year.
	 */
	FILE((project, path) -> path);

	private static final List<LicenseYearSelectionMode> VALUES = List.of(values());
	private final CommitPathReference commitPathReference;

	LicenseYearSelectionMode(CommitPathReference commitPathReference) {
		this.commitPathReference = commitPathReference;
	}

	/**
	 * Gets the year in which the file got created.
	 * <p>
	 * In the case of {@link #PROJECT} the last creation year isn't file dependent.
	 *
	 * @param project the project the file is in
	 * @param path the path to the file
	 * @return the creation year
	 * @throws IOException if the file attributes cannot be read
	 */
	public int getCreationYear(@NotNull Project project, @NotNull Path path) throws IOException {
		Path commitPath = this.commitPathReference.getPathForCommitFetching(project, path);

		if (commitPath != path) {
			return project.getExtensions().getByType(YumiLicenserGradleExtension.class)
					.getProjectCreationYear()
					.get();
		} else {
			Instant instant = Files.readAttributes(commitPath, BasicFileAttributes.class).creationTime().toInstant();
			LocalDate localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault());
			return localDate.getYear();
		}
	}


	/**
	 * Gets the year in which the file got last modified.
	 * <p>
	 * In the case of {@link #PROJECT} the last modification year isn't file dependent.
	 *
	 * @param project the project the file is in
	 * @param path the path to the file
	 * @return the last modification year
	 */
	public int getModificationYear(@NotNull Project project, @NotNull Path path) {
		Path commitPath = this.commitPathReference.getPathForCommitFetching(project, path);
		return GitUtils.getModificationYear(project, commitPath);
	}

	/**
	 * {@return the license year selection mode by its name}
	 *
	 * @param name the name of the mode
	 */
	@Contract(pure = true)
	public static LicenseYearSelectionMode byName(@NotNull String name) {
		for (var mode : VALUES) {
			if (mode.name().equals(name))
				return mode;
		}

		return null;
	}

	@FunctionalInterface
	interface CommitPathReference {
		/**
		 * Gets the path to use to fetch the latest commit.
		 *
		 * @param project the project the path is in
		 * @param path the path
		 * @return the path to use to get the latest commit
		 */
		@NotNull Path getPathForCommitFetching(Project project, Path path);
	}
}