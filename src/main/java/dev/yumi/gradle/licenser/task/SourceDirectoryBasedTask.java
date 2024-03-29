/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task;

import dev.yumi.gradle.licenser.api.comment.HeaderComment;
import dev.yumi.gradle.licenser.api.comment.HeaderCommentManager;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a task that acts on a given source directory set.
 *
 * @author LambdAurora
 * @version 1.1.2
 * @since 1.0.0
 */
@ApiStatus.Internal
public abstract class SourceDirectoryBasedTask extends DefaultTask {
	protected final SourceDirectorySet sourceDirectorySet;
	protected final PatternFilterable patternFilterable;
	protected final boolean excludeBuildDirectory;

	protected SourceDirectoryBasedTask(SourceDirectorySet sourceDirectorySet, PatternFilterable patternFilterable, boolean excludeBuildDirectory) {
		this.sourceDirectorySet = sourceDirectorySet;
		this.patternFilterable = patternFilterable;
		this.excludeBuildDirectory = excludeBuildDirectory;
	}

	/**
	 * Executes the given action to all matched files.
	 *
	 * @param headerCommentManager the header comment manager to find out the header comments of files
	 * @param consumer the action to execute on a given file
	 */
	void execute(HeaderCommentManager headerCommentManager, SourceConsumer consumer) {
		Path buildDir = this.getProject().getLayout().getBuildDirectory().get().getAsFile().toPath();

		this.sourceDirectorySet.matching(this.patternFilterable)
				.visit(fileVisitDetails -> {
					if (fileVisitDetails.isDirectory()) return;

					Path sourcePath = fileVisitDetails.getFile().toPath();

					// Exclude the build directory unless it's forcefully included.
					if (this.excludeBuildDirectory && sourcePath.startsWith(buildDir)) return;

					HeaderComment headerComment = headerCommentManager.findHeaderComment(fileVisitDetails);

					if (headerComment != null) {
						try {
							consumer.consume(
									this.getProject(), this.getLogger(), this.getProject().getProjectDir().toPath(),
									headerComment, sourcePath
							);
						} catch (IOException e) {
							throw new GradleException("Failed to load file " + sourcePath, e);
						}
					}
				});

		consumer.end(this.getLogger());
	}
}