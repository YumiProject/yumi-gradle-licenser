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
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import dev.yumi.gradle.licenser.impl.ValidationError;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.ChangeType;
import org.gradle.work.FileChange;
import org.gradle.work.InputChanges;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Represents a task that checks the validity of license headers in project files.
 *
 * @author LambdAurora
 * @version 2.1.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public abstract class CheckLicenseTask extends SourceDirectoryBasedTask {
	@Inject
	public CheckLicenseTask(YumiLicenserGradleExtension extension) {
		super(extension);
		this.setDescription("Checks whether source files contain a valid license header.");
		this.setGroup("verification");

		if (!this.getLicenseHeader().get().isValid()) {
			this.setEnabled(false);
		}
	}

	@TaskAction
	public void execute(InputChanges inputChanges) {
		this.execute(
				this.getHeaderCommentManager().get(),
				StreamSupport.stream(
								inputChanges.getFileChanges(this.getSourceFiles()).spliterator(),
								false
						).filter(action -> action.getChangeType() != ChangeType.REMOVED)
						.map(FileChange::getFile)
						.map(File::toPath),
				new Consumer(this.getLicenseHeader().get())
		);
	}

	/**
	 * Configures a check task with default values.
	 *
	 * @param ext the licenser extension
	 * @param project the project
	 * @param sourceSet the source set of the files to check
	 * @param sourceSetName the name of the source set
	 * @return the configuration action
	 * @since 2.0.0
	 */
	public static Action<? super CheckLicenseTask> configureDefault(
			YumiLicenserGradleExtension ext,
			Project project,
			SourceDirectorySet sourceSet,
			String sourceSetName
	) {
		return task -> {
			task.setDescription("Checks whether source files in the "
					+ sourceSet.getName()
					+ " source set contain a valid license header."
			);
			task.getSourceFiles().from(
					SourceDirectoryBasedTask.extractFromSourceSet(
							ext,
							project.getLayout().getBuildDirectory().get().getAsFile().toPath(),
							sourceSet
					)
			);
			task.getReportFile().fileValue(
					project.getLayout().getBuildDirectory().get().getAsFile().toPath()
							.resolve("yumi/licenser/check_report_" + sourceSetName + ".txt")
							.toFile()
			);
			boolean excluded = ext.isSourceSetExcluded(sourceSetName);
			task.onlyIf(t -> !excluded);
		};
	}

	class Consumer implements SourceConsumer {
		private final LicenseHeader licenseHeader;
		private final List<FailedCheck> failedChecks = new ArrayList<>();
		private int total = 0;

		public Consumer(LicenseHeader licenseHeader) {
			this.licenseHeader = licenseHeader;
		}

		@Override
		public void consume(
				Path rootDir,
				int projectCreationYear,
				Path buildPath,
				Logger logger,
				Path projectPath,
				HeaderComment headerComment,
				Path path
		) throws IOException {
			var displayPath = projectPath.relativize(path);
			var result = headerComment.readHeaderComment(Files.readString(path));

			if (result.existing() == null) {
				this.failedChecks.add(new FailedCheck(displayPath, List.of("Missing header comment.")));
			} else {
				List<ValidationError> errors = this.licenseHeader.validate(result.existing());
				if (!errors.isEmpty()) {
					this.failedChecks.add(new FailedCheck(
							displayPath,
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
				var message = String.format("All license header checks passed (%d files).", this.total);
				logger.lifecycle(message);
				this.writeReportFile(message + "\n");
			} else {
				var builder = new StringBuilder();

				for (var failedCheck : this.failedChecks) {
					logger.error(" - {} - license checks have failed.", failedCheck.path());
					builder.append(String.format("- %s - license checks have failed.\n", failedCheck.path));
					for (var error : failedCheck.errors()) {
						logger.error("    -> {}", error);
						builder.append(String.format("\t-> %s\n", error));
					}
				}

				this.writeReportFile(String.format("License header checks have failed on %d out of %d files.\n\n%s",
						this.failedChecks.size(), this.total,
						builder
				));

				throw new GradleException(
						String.format("License header checks have failed on %d out of %d files.",
								this.failedChecks.size(), this.total
						)
				);
			}
		}

		private void writeReportFile(CharSequence content) {
			var reportFilePath = CheckLicenseTask.this.getReportFile().get().getAsFile().toPath();
			try {
				Files.createDirectories(reportFilePath.getParent());
				Files.writeString(reportFilePath, content);
			} catch (IOException e) {
				CheckLicenseTask.this.getLogger().error("Failed to create report file.", e);
			}
		}
	}

	record FailedCheck(Path path, List<String> errors) {}
}