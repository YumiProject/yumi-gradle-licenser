/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser;

import dev.yumi.gradle.licenser.compat.KotlinMultiplatformCompat;
import dev.yumi.gradle.licenser.task.ApplyLicenseTask;
import dev.yumi.gradle.licenser.task.CheckLicenseTask;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

/**
 * Represents the Yumi Licenser Gradle plugin.
 *
 * @author LambdAurora
 * @version 2.0.0
 * @since 1.0.0
 */
public class YumiLicenserGradlePlugin implements Plugin<Project> {
	public static final String LICENSE_TASK_SUFFIX = "License";
	public static final String CHECK_TASK_PREFIX = "check";
	public static final String APPLY_TASK_PREFIX = "apply";

	private static final String DEBUG_MODE_PROPERTY = "yumi.gradle.licenser.debug";
	/**
	 * Represents whether the debug mode is enabled or not using the {@value #DEBUG_MODE_PROPERTY} system property.
	 */
	public static final boolean DEBUG_MODE = Boolean.getBoolean(DEBUG_MODE_PROPERTY);

	@Override
	public void apply(Project project) {
		var ext = project.getExtensions().create("license", YumiLicenserGradleExtension.class, project);

		// Register tasks.
		project.getPlugins().withType(JavaBasePlugin.class).configureEach(plugin -> {
			var sourceSets = project.getExtensions().getByType(SourceSetContainer.class);

			sourceSets.matching(sourceSet -> !ext.isSourceSetExcluded(sourceSet))
					.all(sourceSet -> {
						project.getTasks().register(
								getTaskName("check", sourceSet), CheckLicenseTask.class,
								ext
						).configure(CheckLicenseTask.configureDefault(ext, project, sourceSet.getAllSource(), sourceSet.getName()));
						project.getTasks().register(
								getTaskName("apply", sourceSet), ApplyLicenseTask.class,
								ext
						).configure(ApplyLicenseTask.configureDefault(ext, project, sourceSet.getAllSource(), sourceSet.getName()));
					});
		});

		project.getPlugins().withId("org.jetbrains.kotlin.multiplatform", plugin -> {
			try {
				KotlinMultiplatformCompat.applyTasksForKotlinMultiplatform(project, ext);
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		});

		var globalCheck = this.registerGroupedTask(project, CHECK_TASK_PREFIX, task -> {
			task.dependsOn(project.getTasks().withType(CheckLicenseTask.class));

			task.setDescription("Checks whether source files in every source sets contain a valid license header.");
			task.setGroup("verification");
		});
		this.registerGroupedTask(project, APPLY_TASK_PREFIX, task -> {
			task.dependsOn(project.getTasks().withType(ApplyLicenseTask.class));

			task.setDescription("Applies the correct license headers to source files in every source sets.");
			task.setGroup("generation");
		});

		project.getPlugins().withType(LifecycleBasePlugin.class).configureEach(plugin -> {
			project.getTasks().named(LifecycleBasePlugin.CHECK_TASK_NAME).configure(task -> {
				task.dependsOn(globalCheck);
			});
		});
	}

	/**
	 * {@return the task name of a given action for a given source set}
	 *
	 * @param action the task action
	 * @param sourceSet the source set the task is applied to
	 */
	public static String getTaskName(String action, SourceSet sourceSet) {
		if (sourceSet.getName().equals("main")) {
			return action + LICENSE_TASK_SUFFIX + "Main";
		} else {
			return sourceSet.getTaskName(action + LICENSE_TASK_SUFFIX, null);
		}
	}

	private TaskProvider<Task> registerGroupedTask(Project project, String action, Action<Task> consumer) {
		var task = project.getTasks().register(action + LICENSE_TASK_SUFFIX + 's');
		task.configure(consumer);
		return task;
	}
}
