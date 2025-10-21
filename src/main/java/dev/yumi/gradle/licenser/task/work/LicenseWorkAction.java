/*
 * Copyright 2025 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.task.work;

import dev.yumi.gradle.licenser.api.comment.HeaderComment;
import dev.yumi.gradle.licenser.api.comment.HeaderCommentManager;
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import dev.yumi.gradle.licenser.impl.LogConsumer;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a work action related to licensing.
 *
 * @author LambdAurora
 * @version 2.2.0
 * @since 2.2.0
 */
public abstract class LicenseWorkAction
		implements WorkAction<LicenseWorkAction.Parameters> {
	@Override
	public final void execute() {
		var params = this.getParameters();

		Path sourcePath = params.getSourceFile().get().getAsFile().toPath();

		HeaderComment headerComment = params.getHeaderCommentManager().get().findHeaderComment(sourcePath);

		if (headerComment != null) {
			try {
				var context = new Context(sourcePath, headerComment, params);

				this.execute(context);

				context.finish(params.getReportFile().get().getAsFile().toPath());
			} catch (IOException e) {
				throw new GradleException("Failed to load file " + sourcePath, e);
			}
		}
	}

	protected abstract void execute(Context context) throws IOException;

	protected static class Context implements LogConsumer {
		public final Path sourcePath;
		public final HeaderComment headerComment;
		public final LicenseHeader licenseHeader;
		public final Path rootDir;
		public final Path projectDir;
		public final Path buildDir;
		public final int projectCreationYear;
		private final boolean debugMode;

		private final List<String> logs = new ArrayList<>();

		private Report.Details details = Report.NoDetails.INSTANCE;

		public Context(
				Path sourcePath, HeaderComment headerComment, Parameters params
		) {
			this.sourcePath = sourcePath;
			this.headerComment = headerComment;

			this.licenseHeader = params.getLicenseHeader().get();

			this.rootDir = Path.of(params.getRootDirectory().get());
			this.projectDir = Path.of(params.getProjectDirectory().get());
			this.buildDir = Path.of(params.getBuildDirectory().get());

			this.projectCreationYear = params.getProjectCreationYear().get();
			this.debugMode = params.getDebugMode().get();
		}

		@Override
		public void log(String message, Object... args) {
			if (this.debugMode) {
				this.logs.add(String.format(message, args));
			}
		}

		public void acceptReport(Report.Details details) {
			this.details = details;
		}

		private void finish(Path reportFile) throws IOException {
			try (
					var fileOut = Files.newOutputStream(reportFile);
					var objectOut = new ObjectOutputStream(fileOut)
			) {
				objectOut.writeObject(new Report<>(this.logs, details));
			}
		}
	}

	public record Report<T extends Report.Details>(List<String> logs, T details) implements Serializable {
		public interface Details extends Serializable {}

		public record NoDetails() implements Details {
			public static final NoDetails INSTANCE = new NoDetails();
		}
	}

	public interface Parameters extends WorkParameters {
		RegularFileProperty getSourceFile();

		/**
		 * {@return the license header property to use for this work action}
		 */
		Property<LicenseHeader> getLicenseHeader();

		/**
		 * {@return the header comment manager property}
		 */
		Property<HeaderCommentManager> getHeaderCommentManager();

		/**
		 * {@return the root directory path property}
		 */
		Property<String> getRootDirectory();

		/**
		 * {@return the project's directory path property}
		 */
		Property<String> getProjectDirectory();

		/**
		 * {@return the build directory path property}
		 */
		Property<String> getBuildDirectory();

		/**
		 * {@return the project's creation year property}
		 */
		Property<Integer> getProjectCreationYear();

		RegularFileProperty getReportFile();

		Property<Boolean> getDebugMode();
	}
}
