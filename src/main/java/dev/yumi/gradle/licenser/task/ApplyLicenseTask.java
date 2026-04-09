/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task;

import dev.yumi.gradle.licenser.YumiLicenserGradlePlugin;
import dev.yumi.gradle.licenser.task.work.ApplyLicenseWorkAction;
import dev.yumi.gradle.licenser.task.work.ApplyLicenseWorkAction.ApplyReportDetails;
import dev.yumi.gradle.licenser.task.work.LicenseWorkAction.Report;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.workers.WorkerExecutor;
import org.jetbrains.annotations.ApiStatus;

import javax.inject.Inject;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.stream.StreamSupport;

/**
 * Represents the task that applies license headers to project files.
 *
 * @author LambdAurora
 * @version 3.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
@UntrackedTask(because = "Task may rewrite the input files.")
public abstract class ApplyLicenseTask extends SourceDirectoryBasedTask {
	@Inject
	public ApplyLicenseTask() {
		super();
		this.setDescription("Applies the correct license headers to source files.");
		this.setGroup("generation");

		if (!this.getLicenseHeader().get().isValid()) {
			this.setEnabled(false);
		}
	}

	@Inject
	public abstract WorkerExecutor getWorkerExecutor();

	@SuppressWarnings("unchecked")
	@TaskAction
	public void execute() throws IOException, ClassNotFoundException, NoSuchAlgorithmException {
		var workQueue = this.getWorkerExecutor().noIsolation();
		var files = StreamSupport.stream(this.getEffectiveSourceFiles().spliterator(), false).toList();

		var tempDir = Files.createTempDirectory("yumi-gradle-licenser-workers-");
		var reportPaths = new LinkedHashMap<Path, Path>();

		var digest = MessageDigest.getInstance("SHA-256");

		for (var file : files) {
			var hashBytes = digest.digest(file.toPath().toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
			var nameHash = HexFormat.of().formatHex(hashBytes);

			var reportPath = tempDir.resolve(nameHash);
			reportPaths.put(file.toPath(), reportPath);

			workQueue.submit(ApplyLicenseWorkAction.class, params -> {
				params.getSourceFile().set(file);
				params.getLicenseHeader().set(this.getLicenseHeader());
				params.getHeaderCommentManager().set(this.getHeaderCommentManager());
				params.getRootDirectory().set(this.getRootDirectory());
				params.getProjectDirectory().set(this.getProjectDirectory());
				params.getBuildDirectory().set(this.getBuildDirectory());
				params.getProjectCreationYear().set(this.getProjectCreationYear());
				params.getReportFile().set(reportPath.toFile());
				params.getDebugMode().set(YumiLicenserGradlePlugin.DEBUG_MODE);
			});
		}

		workQueue.await();

		var reports = new LinkedHashMap<Path, Report<ApplyReportDetails>>();
		var toClean = new ArrayList<Path>();
		int total = 0;

		var logger = this.getLogger();

		for (var reportPath : reportPaths.entrySet()) {
			if (Files.exists(reportPath.getValue())) {
				toClean.add(reportPath.getValue());
				total++;

				try (
						var fileIn = Files.newInputStream(reportPath.getValue());
						var objectIn = new ObjectInputStream(fileIn)
				) {
					var report = (Report<ApplyReportDetails>) objectIn.readObject();
					report.logs().forEach(line -> logger.lifecycle("{}", line));
					reports.put(reportPath.getKey(), report);
				}
			}
		}

		int updated = 0;

		for (var entry : reports.entrySet()) {
			if (entry.getValue().details().updated()) {
				logger.lifecycle(" - Updated file {}", entry.getKey());
				updated++;
			}
		}

		logger.lifecycle("Updated {} out of {} files.", updated, total);

		for (var path : toClean) {
			Files.delete(path);
		}
		Files.delete(tempDir);
	}
}
