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
import dev.yumi.gradle.licenser.api.comment.HeaderCommentManager;
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import dev.yumi.gradle.licenser.util.Utils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the task that applies license headers to project files.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public class ApplyLicenseTask extends JavaSourceBasedTask {
	private final LicenseHeader licenseHeader;
	private final HeaderCommentManager headerCommentManager;

	@Inject
	public ApplyLicenseTask(SourceSet sourceSet, YumiLicenserGradleExtension extension) {
		super(sourceSet, extension.asPatternFilterable());
		this.licenseHeader = extension.getLicenseHeader();
		this.headerCommentManager = extension.getHeaderCommentManager();
		this.setDescription("Applies the correct license headers to source files in the " + sourceSet.getName() + " source set.");
		this.setGroup("generation");

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
		private final List<Path> updatedFiles = new ArrayList<>();
		private int total = 0;

		public Consumer(LicenseHeader licenseHeader) {
			this.licenseHeader = licenseHeader;
		}

		@Override
		public void consume(Project project, Logger logger, Path rootPath, HeaderComment headerComment, Path path) throws IOException {
			if (YumiLicenserGradlePlugin.DEBUG_MODE) {
				logger.lifecycle("=> Visiting {}...", path);
			}

			String read = Files.readString(path, StandardCharsets.UTF_8);
			var readComment = headerComment.readHeaderComment(read);

			List<String> lines = this.licenseHeader.format(project, logger, path, readComment.existing());

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

				String content = start + headerComment.writeHeaderComment(lines, readComment.separator()) + end;

				try {
					var backupPath = Utils.getBackupPath(project, rootPath, path);

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