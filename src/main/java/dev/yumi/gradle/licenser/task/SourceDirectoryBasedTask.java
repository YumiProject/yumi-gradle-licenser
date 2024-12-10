/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task;

import dev.yumi.gradle.licenser.YumiLicenserGradleExtension;
import dev.yumi.gradle.licenser.api.comment.HeaderComment;
import dev.yumi.gradle.licenser.api.comment.HeaderCommentManager;
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents a task that acts on a given source directory set.
 *
 * @author LambdAurora
 * @version 2.1.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public abstract class SourceDirectoryBasedTask extends DefaultTask {
	protected SourceDirectoryBasedTask(YumiLicenserGradleExtension extension) {
		this.getLicenseHeader().convention(extension.getLicenseHeader());
		this.getHeaderCommentManager().convention(extension.getHeaderCommentManager());
		this.getRootDirectory().convention(this.getProject().getRootDir().toString());
		this.getProjectDirectory().convention(this.getProject().getProjectDir().toString());
		this.getProjectCreationYear().convention(extension.getProjectCreationYear());
		this.getBuildDirectory().convention(this.getProject().getLayout().getBuildDirectory().get().toString());
	}

	/**
	 * {@return the source files property that will be affected by this task}
	 */
	@InputFiles
	@SkipWhenEmpty
	@IgnoreEmptyDirectories
	public abstract ConfigurableFileCollection getSourceFiles();

	/**
	 * {@return the license header property to use for this task}
	 */
	@Input
	public abstract Property<LicenseHeader> getLicenseHeader();

	/**
	 * {@return the header comment manager property}
	 */
	@Input
	public abstract Property<HeaderCommentManager> getHeaderCommentManager();

	/**
	 * {@return the root directory path property}
	 */
	@Input
	public abstract Property<String> getRootDirectory();

	/**
	 * {@return the project's directory path property}
	 */
	@Input
	public abstract Property<String> getProjectDirectory();

	/**
	 * {@return the project's creation year property}
	 */
	@Input
	public abstract Property<Integer> getProjectCreationYear();

	/**
	 * {@return the build directory path property}
	 */
	@Input
	public abstract Property<String> getBuildDirectory();

	@OutputFile
	public abstract RegularFileProperty getReportFile();

	/**
	 * Executes the given action to all matched files.
	 *
	 * @param headerCommentManager the header comment manager to find out the header comments of files
	 * @param sourceFiles the source files to treat in this task
	 * @param consumer the action to execute on a given file
	 */
	void execute(HeaderCommentManager headerCommentManager, Stream<Path> sourceFiles, SourceConsumer consumer) {
		Path rootDir = Path.of(this.getRootDirectory().get());
		Path projectDir = Path.of(this.getProjectDirectory().get());
		Path buildDir = Path.of(this.getBuildDirectory().get());

		sourceFiles.forEach(sourcePath -> {
			HeaderComment headerComment = headerCommentManager.findHeaderComment(sourcePath);

			if (headerComment != null) {
				try {
					consumer.consume(
							rootDir,
							this.getProjectCreationYear().get(),
							buildDir,
							this.getLogger(),
							projectDir,
							headerComment,
							sourcePath
					);
				} catch (IOException e) {
					throw new GradleException("Failed to load file " + sourcePath, e);
				}
			}
		});

		consumer.end(this.getLogger());
	}

	public static Set<File> extractFromSourceSet(YumiLicenserGradleExtension ext, Path buildPath, SourceDirectorySet sourceDirectorySet) {
		boolean excludeBuildDir = ext.getExcludeBuildDirectory().get();
		return sourceDirectorySet
				.matching(ext.asPatternFilterable())
				// Exclude the build directory unless it's forcefully included.
				.filter(file -> !excludeBuildDir || !file.toPath().startsWith(buildPath))
				.getFiles();
	}
}
