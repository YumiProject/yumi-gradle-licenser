/*
 * Copyright 2025 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task.work;

import dev.yumi.gradle.licenser.util.Utils;
import org.gradle.api.GradleException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Represents a work action related to license application.
 *
 * @author LambdAurora
 * @version 2.2.0
 * @since 2.2.0
 */
public abstract class ApplyLicenseWorkAction extends LicenseWorkAction {
	@Override
	protected void execute(Context context) throws IOException {
		context.log("=> Visiting %s...", context.sourcePath);

		String read = Files.readString(context.sourcePath, StandardCharsets.UTF_8);
		var readComment = context.headerComment.readHeaderComment(read);

		List<String> lines = context.licenseHeader.format(
				context.rootDir, context.projectCreationYear, context, context.sourcePath, readComment.existing()
		);

		boolean updated = false;

		if (lines != null) {
			updated = true;

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
					+ context.headerComment.writeHeaderComment(lines, readComment.separator())
					+ end;

			try {
				var backupPath = Utils.getBackupPath(context.buildDir, context.projectDir, context.sourcePath);

				if (backupPath == null) {
					throw new GradleException("Cannot backup file " + context.sourcePath + ", abandoning formatting.");
				}

				if (!Files.isDirectory(backupPath.getParent())) {
					Files.createDirectories(backupPath.getParent());
				}

				Files.copy(context.sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw new GradleException("Cannot backup file " + context.sourcePath + ", abandoning formatting.", e);
			}

			Files.writeString(context.sourcePath, content, StandardCharsets.UTF_8);
		}

		context.acceptReport(new ApplyReportDetails(updated));
	}

	public record ApplyReportDetails(boolean updated) implements Report.Details {
	}
}
