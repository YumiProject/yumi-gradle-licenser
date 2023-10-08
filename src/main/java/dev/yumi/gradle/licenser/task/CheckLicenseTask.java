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
import dev.yumi.gradle.licenser.impl.ValidationError;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a task that checks the validity of license headers in project files.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public class CheckLicenseTask extends JavaSourceBasedTask {
	private final LicenseHeader licenseHeader;
	private final HeaderCommentManager headerCommentManager;

	@Inject
	public CheckLicenseTask(SourceSet sourceSet, YumiLicenserGradleExtension extension) {
		super(sourceSet, extension.asPatternFilterable());
		this.licenseHeader = extension.getLicenseHeader();
		this.headerCommentManager = extension.getHeaderCommentManager();
		this.setDescription("Checks whether source files in the " + sourceSet.getName() + " source set contain a valid license header.");
		this.setGroup("verification");

		if (!this.licenseHeader.isValid()) {
			this.setEnabled(false);
		}
	}

	@TaskAction
	public void execute() {
		this.execute(this.headerCommentManager, new Consumer(this.licenseHeader));
	}

	static class Consumer implements SourceConsumer {
		private final LicenseHeader licenseHeader;
		private final List<FailedCheck> failedChecks = new ArrayList<>();
		private int total = 0;

		public Consumer(LicenseHeader licenseHeader) {
			this.licenseHeader = licenseHeader;
		}

		@Override
		public void consume(Project project, Logger logger, Path rootPath, HeaderComment headerComment, Path path) throws IOException {
			var result = headerComment.readHeaderComment(Files.readString(path));

			if (result.existing() == null) {
				this.failedChecks.add(new FailedCheck(path, List.of("Missing header comment.")));
			} else {
				List<ValidationError> errors = this.licenseHeader.validate(result.existing());
				if (!errors.isEmpty()) {
					this.failedChecks.add(new FailedCheck(
							path,
							errors.stream()
									.map(error -> error.headerRule() + ": " + error.error().getMessage())
									.toList()
					));
				}
			}

			this.total++;
		}

		@Override
		public void end(Logger logger) {
			if (this.failedChecks.isEmpty()) {
				logger.lifecycle("All license header checks passed ({} files).", this.total);
			} else {
				for (var failedCheck : this.failedChecks) {
					logger.error(" - {} - license checks have failed.", failedCheck.path());
					for (var error : failedCheck.errors()) {
						logger.error("    -> {}", error);
					}
				}

				throw new GradleException(
						String.format("License header checks have failed on %s out of %d files.",
								this.failedChecks.size(), this.total
						)
				);
			}
		}
	}

	record FailedCheck(Path path, List<String> errors) {}
}