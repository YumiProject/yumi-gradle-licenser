/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.compat;

import dev.yumi.gradle.licenser.YumiLicenserGradleExtension;
import dev.yumi.gradle.licenser.YumiLicenserGradlePlugin;
import dev.yumi.gradle.licenser.task.ApplyLicenseTask;
import dev.yumi.gradle.licenser.task.CheckLicenseTask;
import dev.yumi.gradle.licenser.util.Utils;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.file.SourceDirectorySet;
import org.jetbrains.annotations.ApiStatus;

import java.lang.invoke.MethodHandles;

/**
 * Provides compatibility code to natively handle Kotlin multiplatform source sets.
 * <p>
 * No direct reference to the Kotlin multiplatform plugin is made due to classloader isolation issues.
 *
 * @author LambdAurora
 * @version 1.1.1
 * @since 1.1.0
 */
@ApiStatus.Internal
public final class KotlinMultiplatformCompat {
	private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

	private KotlinMultiplatformCompat() {
		throw new UnsupportedOperationException("KotlinMultiplatformCompat only contains static definitions.");
	}

	public static void applyTasksForKotlinMultiplatform(Project project, YumiLicenserGradleExtension ext) throws Throwable {
		Utils.debugLog(project, "Found Kotlin Multiplatform plugin, applying special configuration...");

		var kotlinExt = project.getExtensions().getByName("kotlin"); // Kotlin multiplatform has a Kotlin extension.

		// We get the sourceSets of the Kotlin multiplatform plugin.
		var getSourceSets = LOOKUP.unreflect(kotlinExt.getClass().getMethod("getSourceSets"));
		var sourceSets = (NamedDomainObjectContainer<?>) getSourceSets.invoke(kotlinExt);

		// For each source set we check whether they're excluded then add the associated tasks.
		sourceSets.all(sourceSet -> {
			try {
				var name = (String) LOOKUP.unreflect(sourceSet.getClass().getMethod("getName")).invoke(sourceSet);

				if (ext.isSourceSetExcluded(name)) return;

				var kotlinSet = (SourceDirectorySet) LOOKUP.unreflect(sourceSet.getClass().getMethod("getKotlin"))
						.invoke(sourceSet);

				project.getTasks().register(
						getTaskName("check", name), CheckLicenseTask.class,
						kotlinSet, ext
				).configure(task -> task.onlyIf(t -> !ext.isSourceSetExcluded(name)));
				project.getTasks().register(
						getTaskName("apply", name), ApplyLicenseTask.class,
						kotlinSet, ext
				).configure(task -> task.onlyIf(t -> !ext.isSourceSetExcluded(name)));
			} catch (Throwable e) {
				throw new RuntimeException(e);
			}
		});
	}

	private static String getTaskName(String action, String sourceSetName) {
		return action + YumiLicenserGradlePlugin.LICENSE_TASK_SUFFIX + Character.toUpperCase(sourceSetName.charAt(0))
				+ (sourceSetName.length() > 1 ? sourceSetName.substring(1) : "");
	}
}
