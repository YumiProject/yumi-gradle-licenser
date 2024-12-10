/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task;

import dev.yumi.gradle.licenser.YumiLicenserGradleExtension;
import dev.yumi.gradle.licenser.YumiLicenserGradlePlugin;
import dev.yumi.gradle.licenser.api.comment.HeaderComment;
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import dev.yumi.gradle.licenser.util.Utils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Represents the task that applies license headers to project files.
 *
 * @author LambdAurora
 * @version 2.1.0
 * @since 1.0.0
 */
@ApiStatus.Internal
@UntrackedTask(because = "Task may rewrite the input files.")
public abstract class ApplyLicenseTask extends SourceDirectoryBasedTask {
	@Inject
	public ApplyLicenseTask(YumiLicenserGradleExtension extension) {
		super(extension);
		this.setDescription("Applies the correct license headers to source files.");
		this.setGroup("generation");

		if (!this.getLicenseHeader().get().isValid()) {
			this.setEnabled(false);
		}
	}

	@TaskAction
	public void execute() {
		this.execute(
				this.getHeaderCommentManager().get(),
				StreamSupport.stream(this.getSourceFiles().spliterator(), false).map(File::toPath),
				new Consumer(this.getLicenseHeader().get())
		);
	}

	/**
	 * Configures an apply task with default values.
	 *
	 * @param ext the licenser extension
	 * @param project the project
	 * @param sourceSet the source set of the files to apply to
	 * @param sourceSetName the name of the source set
	 * @return the configuration action
	 * @since 2.0.0
	 */
	public static Action<? super ApplyLicenseTask> configureDefault(
			YumiLicenserGradleExtension ext,
			Project project,
			SourceDirectorySet sourceSet,
			String sourceSetName
	) {
		return task -> {
			task.setDescription("Applies the correct license headers to source files in the "
					+ sourceSet.getName()
					+ " source set."
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
							.resolve("yumi/licenser/apply_report_" + sourceSetName + ".txt")
							.toFile()
			);
			boolean excluded = ext.isSourceSetExcluded(sourceSetName);
			task.onlyIf(t -> !excluded);
		};
	}

	static class Consumer implements SourceConsumer {
		private final LicenseHeader licenseHeader;
		private final List<Path> updatedFiles = new ArrayList<>();
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
			if (YumiLicenserGradlePlugin.DEBUG_MODE) {
				logger.lifecycle("=> Visiting {}...", path);
			}

			String read = Files.readString(path, StandardCharsets.UTF_8);
			var readComment = headerComment.readHeaderComment(read);

			List<String> lines = this.licenseHeader.format(
					rootDir, projectCreationYear, logger, path, readComment.existing()
			);

			if (lines != null) {
				this.updatedFiles.add(path);

				String start = "";

				if (readComment.start() != 0) {
					start = read.substring(0, readComment.start());

					if (start.isBlank()) start = "";
				}

				String end = read.substring(readComment.end());

				if (readComment.start() == readComment.end() && readComment.start() == 0) {
					end = readComment.separator() + readComment.separator() + end;
				}

				String content = start
						+ headerComment.writeHeaderComment(lines, readComment.separator())
						+ end;

				try {
					var backupPath = Utils.getBackupPath(buildPath, projectPath, path);

					if (backupPath == null) {
						throw new GradleException("Cannot backup file " + path + ", abandoning formatting.");
					}

					if (!Files.isDirectory(backupPath.getParent())) {
						Files.createDirectories(backupPath.getParent());
					}

					Files.copy(path, backupPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e) {
					throw new GradleException("Cannot backup file " + path + ", abandoning formatting.", e);
				}

				Files.writeString(path, content, StandardCharsets.UTF_8);
			}

			this.total++;
		}

		@Override
		public void end(Logger logger) {
			for (var path : this.updatedFiles) {
				logger.lifecycle(" - Updated file {}", path);
			}

			logger.lifecycle("Updated {} out of {} files.", this.updatedFiles.size(), this.total);
		}
	}
}