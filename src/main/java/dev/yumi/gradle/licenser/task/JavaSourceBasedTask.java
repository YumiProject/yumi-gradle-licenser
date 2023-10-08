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
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.util.PatternFilterable;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Represents a task that acts on a given Java project source set.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public abstract class JavaSourceBasedTask extends DefaultTask {
	protected final SourceSet sourceSet;
	protected final PatternFilterable patternFilterable;

	protected JavaSourceBasedTask(SourceSet sourceSet, PatternFilterable patternFilterable) {
		this.sourceSet = sourceSet;
		this.patternFilterable = patternFilterable;
	}

	/**
	 * Executes the given action to all matched files.
	 *
	 * @param headerCommentManager the header comment manager to find out the header comments of files
	 * @param consumer the action to execute on a given file
	 */
	void execute(HeaderCommentManager headerCommentManager, SourceConsumer consumer) {
		this.sourceSet.getAllSource()
				.matching(this.patternFilterable)
				.visit(fileVisitDetails -> {
					if (fileVisitDetails.isDirectory()) return;

					HeaderComment headerComment = headerCommentManager.findHeaderComment(fileVisitDetails);

					if (headerComment != null) {
						Path sourcePath = fileVisitDetails.getFile().toPath();

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