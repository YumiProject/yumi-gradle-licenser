/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser;

import dev.yumi.gradle.licenser.api.comment.HeaderCommentManager;
import dev.yumi.gradle.licenser.api.rule.HeaderParseException;
import dev.yumi.gradle.licenser.api.rule.HeaderRule;
import dev.yumi.gradle.licenser.impl.LicenseHeader;
import dev.yumi.gradle.licenser.task.ApplyLicenseTask;
import dev.yumi.gradle.licenser.task.CheckLicenseTask;
import dev.yumi.gradle.licenser.task.SourceDirectoryBasedTask;
import dev.yumi.gradle.licenser.util.Utils;
import groovy.lang.Closure;
import groovy.lang.Delegate;
import groovy.transform.PackageScope;
import org.eclipse.jgit.util.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.Contract;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Represents the Yumi Licenser Gradle extension to configure the plugin in buildscripts.
 *
 * @author LambdAurora
 * @version 3.0.0
 * @since 1.0.0
 */
public abstract class YumiLicenserGradleExtension implements PatternFilterable {
	//region Properties

	@PackageScope
	final LicenseHeader header = new LicenseHeader(new ArrayList<>());

	@PackageScope
	final HeaderCommentManager headerCommentManager = new HeaderCommentManager();

	@PackageScope
	final Property<Integer> projectCreationYear;

	//region File selection

	/**
	 * The filter to apply to the source files.
	 * <p>
	 * By default, this only includes a few excludes for binary files or files without standardized comment formats.
	 */
	@Delegate
	public PatternFilterable patternFilterable;

	@PackageScope
	final List<String> excludedSourceSets = new ArrayList<>();

	@PackageScope
	final Property<Boolean> excludeBuildDirectory;

	//endregion
	//endregion

	//region Utils

	@PackageScope
	final TextResourceFactory textResources;

	//endregion

	@Inject
	public YumiLicenserGradleExtension(final ObjectFactory objects, final Project project) {
		this.textResources = project.getResources().getText();

		this.projectCreationYear = objects.property(Integer.class)
				.convention(project.provider(() -> Utils.getProjectCreationYear(project)));

		this.patternFilterable = new PatternSet();
		this.exclude(
				"**/*.txt",
				"**/*.json",
				"**/*.yml",

				// Image files.
				"**/*.apng",
				"**/*.bmp",
				"**/*.gif",
				"**/*.ico",
				"**/*.jpg",
				"**/*.png",
				"**/*.qoi",
				"**/*.webp",

				// Binary files.
				"**/*.bin",
				"**/*.class",
				"**/*.jar",
				"**/*.tar",
				"**/*.war",
				"**/*.zip",

				// Manifest
				"**/MANIFEST.MF",
				"**/META-INF/services/**"
		);
		this.excludeBuildDirectory = objects.property(Boolean.class)
				.convention(true);
	}

	@Inject
	protected abstract TaskContainer getTasks();

	/**
	 * Adds a rule from a file.
	 *
	 * @param header the file
	 */
	public void rule(Object header) {
		String name;
		List<String> lines;

		if (header instanceof Path path) {
			name = path.toString();

			try {
				lines = Files.readAllLines(path);
			} catch (IOException e) {
				throw new GradleException(String.format("Failed to load license header %s", path), e);
			}
		} else {
			Path path = this.textResources.fromFile(header).asFile().toPath();
			name = path.toString();

			try {
				lines = Files.readAllLines(path);
			} catch (IOException e) {
				throw new GradleException(String.format("Failed to load license header %s", path), e);
			}
		}

		try {
			this.rule(HeaderRule.parse(name, lines));
		} catch (HeaderParseException e) {
			throw new GradleException(String.format("Failed to load license header %s", header), e);
		}
	}

	/**
	 * Adds a license header rule.
	 *
	 * @param rule the license header rule
	 */
	public void rule(HeaderRule rule) {
		this.header.addRule(rule);
	}

	/**
	 * {@return the license header definition of this project}
	 */
	@Contract(pure = true)
	public LicenseHeader getLicenseHeader() {
		return this.header;
	}

	/**
	 * {@return the header comment manager to attach header comment to specific file formats}
	 */
	@Contract(pure = true)
	public HeaderCommentManager getHeaderCommentManager() {
		return this.headerCommentManager;
	}

	/**
	 * {@return the project creation year property}
	 */
	@Contract(pure = true)
	public Property<Integer> getProjectCreationYear() {
		return this.projectCreationYear;
	}

	/**
	 * {@return the delegated filterable pattern}
	 */
	@Contract(pure = true)
	public PatternFilterable asPatternFilterable() {
		return this.patternFilterable;
	}

	@Override
	public Set<String> getIncludes() {
		return this.patternFilterable.getIncludes();
	}

	@Override
	public Set<String> getExcludes() {
		return this.patternFilterable.getExcludes();
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension setIncludes(Iterable<String> includes) {
		this.patternFilterable.setIncludes(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension setExcludes(Iterable<String> excludes) {
		this.patternFilterable.setExcludes(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension include(String... includes) {
		this.patternFilterable.include(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension include(Iterable<String> includes) {
		this.patternFilterable.include(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension include(Spec<FileTreeElement> includeSpec) {
		this.patternFilterable.include(includeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension include(Closure includeSpec) {
		this.patternFilterable.include(includeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension exclude(String... excludes) {
		this.patternFilterable.exclude(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension exclude(Iterable<String> excludes) {
		this.patternFilterable.exclude(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension exclude(Spec<FileTreeElement> excludeSpec) {
		this.patternFilterable.exclude(excludeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public YumiLicenserGradleExtension exclude(Closure excludeSpec) {
		this.patternFilterable.exclude(excludeSpec);
		return this;
	}

	/**
	 * Excludes an entire source set.
	 *
	 * @param sourceSetName the source set name
	 * @return {@code this}
	 * @see #exclude(SourceSet)
	 * @since 1.1.0
	 */
	@Contract("_ -> this")
	public YumiLicenserGradleExtension excludeSourceSet(String sourceSetName) {
		this.excludedSourceSets.add(sourceSetName);
		return this;
	}

	/**
	 * Excludes an entire source set.
	 *
	 * @param sourceSet the source set
	 * @return {@code this}
	 * @see #excludeSourceSet(String)
	 */
	@Contract("_ -> this")
	public YumiLicenserGradleExtension exclude(SourceSet sourceSet) {
		return this.excludeSourceSet(sourceSet.getName());
	}

	/**
	 * {@return {@code true} if the source set is excluded, or {@code false} otherwise}
	 *
	 * @param sourceSetName the name of the source set to check
	 * @see #isSourceSetExcluded(SourceSet)
	 * @since 1.1.0
	 */
	public boolean isSourceSetExcluded(String sourceSetName) {
		return this.excludedSourceSets.contains(sourceSetName);
	}

	/**
	 * {@return {@code true} if the source set is excluded, or {@code false} otherwise}
	 *
	 * @param sourceSet the source set to check
	 * @see #isSourceSetExcluded(String)
	 */
	public boolean isSourceSetExcluded(SourceSet sourceSet) {
		return this.isSourceSetExcluded(sourceSet.getName());
	}

	/**
	 * {@return the property which excludes the build directory from checks if set to {@code true}}
	 *
	 * @since 1.1.2
	 */
	@Contract(pure = true)
	public Property<Boolean> getExcludeBuildDirectory() {
		return this.excludeBuildDirectory;
	}

	/**
	 * Registers check and apply licenses tasks with the given name, for a given collection of source files.
	 *
	 * @param name the name of the pair of tasks to register
	 * @param sourceFiles the source files
	 * @see #registerTasks(String, FileCollection)
	 * @see #registerTasks(String, Action)
	 * @since 3.0.0
	 */
	public void registerTasks(String name, Object... sourceFiles) {
		this.registerTasks(name, task -> task.getSourceFiles().from(sourceFiles));
	}

	/**
	 * Registers check and apply licenses tasks with the given name, for a given collection of source files.
	 *
	 * @param name the name of the pair of tasks to register
	 * @param sourceFiles the source files
	 * @see #registerTasks(String, Object...)
	 * @see #registerTasks(String, Action)
	 * @since 3.0.0
	 */
	public void registerTasks(String name, FileCollection sourceFiles) {
		this.registerTasks(name, task -> task.getSourceFiles().from(sourceFiles));
	}

	/**
	 * Registers check and apply licenses tasks with the given name, configured using the provided action.
	 *
	 * @param name the name of the pair of tasks to register
	 * @param action the configuration action
	 * @see #registerTasks(String, Object...)
	 * @see #registerTasks(String, FileCollection)
	 * @since 3.0.0
	 */
	public void registerTasks(String name, Action<SourceDirectoryBasedTask> action) {
		var checkTask = this.getTasks().register(this.getTaskName("check", name), CheckLicenseTask.class);
		checkTask.configure(task -> {
			task.setDescription("Checks whether source files in the " + name + " source set contain a valid license header.");
			action.execute(task);
		});
		var applyTask = this.getTasks().register(this.getTaskName("apply", name), ApplyLicenseTask.class);
		applyTask.configure(task -> {
			task.setDescription("Applies the correct license headers to source files in the " + name + " source set.");
			action.execute(task);
		});
	}

	/**
	 * {@return the task name of a given action for a given source set name}
	 *
	 * @param action the task action
	 * @param name the name of the source set
	 */
	private String getTaskName(String action, String name) {
		return action + YumiLicenserGradlePlugin.LICENSE_TASK_SUFFIX + StringUtils.capitalize(name);
	}
}
