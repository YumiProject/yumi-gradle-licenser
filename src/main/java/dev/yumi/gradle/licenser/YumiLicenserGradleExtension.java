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
import dev.yumi.gradle.licenser.util.Utils;
import groovy.lang.Closure;
import groovy.lang.Delegate;
import groovy.transform.PackageScope;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.resources.TextResourceFactory;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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
 * @version 1.1.2
 * @since 1.0.0
 */
public class YumiLicenserGradleExtension implements PatternFilterable {
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

	/**
	 * Adds a rule from a file.
	 *
	 * @param header the file
	 */
	public void rule(@NotNull Object header) {
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
	public @NotNull HeaderCommentManager getHeaderCommentManager() {
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
	public @NotNull Set<String> getIncludes() {
		return this.patternFilterable.getIncludes();
	}

	@Override
	public @NotNull Set<String> getExcludes() {
		return this.patternFilterable.getExcludes();
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension setIncludes(@NotNull Iterable<String> includes) {
		this.patternFilterable.setIncludes(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension setExcludes(@NotNull Iterable<String> excludes) {
		this.patternFilterable.setExcludes(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension include(String @NotNull ... includes) {
		this.patternFilterable.include(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension include(@NotNull Iterable<String> includes) {
		this.patternFilterable.include(includes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension include(@NotNull Spec<FileTreeElement> includeSpec) {
		this.patternFilterable.include(includeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension include(@NotNull Closure includeSpec) {
		this.patternFilterable.include(includeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension exclude(String @NotNull ... excludes) {
		this.patternFilterable.exclude(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension exclude(Iterable<String> excludes) {
		this.patternFilterable.exclude(excludes);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension exclude(@NotNull Spec<FileTreeElement> excludeSpec) {
		this.patternFilterable.exclude(excludeSpec);
		return this;
	}

	@Contract("_ -> this")
	@Override
	public @NotNull YumiLicenserGradleExtension exclude(@NotNull Closure excludeSpec) {
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
	public @NotNull YumiLicenserGradleExtension excludeSourceSet(@NotNull String sourceSetName) {
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
	public @NotNull YumiLicenserGradleExtension exclude(@NotNull SourceSet sourceSet) {
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
	public @NotNull Property<Boolean> getExcludeBuildDirectory() {
		return this.excludeBuildDirectory;
	}
}
