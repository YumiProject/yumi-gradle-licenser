/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.impl;

import dev.yumi.gradle.licenser.YumiLicenserGradlePlugin;
import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;
import dev.yumi.gradle.licenser.api.rule.HeaderRule;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents the valid license headers for this project.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public final class LicenseHeader {
	private final List<HeaderRule> rules;

	public LicenseHeader(HeaderRule... rules) {
		this(new ArrayList<>(List.of(rules)));
	}

	public LicenseHeader(List<HeaderRule> rules) {
		this.rules = rules;
	}

	/**
	 * {@return {@code true} if this license header is valid and can be used for validation, otherwise {@code false}}
	 */
	public boolean isValid() {
		return !this.rules.isEmpty();
	}

	/**
	 * Adds a header rule.
	 *
	 * @param rule the rule to add
	 */
	public void addRule(HeaderRule rule) {
		this.rules.add(rule);
	}

	/**
	 * Validates the given file.
	 *
	 * @param header the existing header
	 * @return a list of validation errors if there's any
	 */
	public @NotNull List<ValidationError> validate(@NotNull List<String> header) {
		var errors = new ArrayList<ValidationError>();

		for (var rule : this.rules) {
			var result = rule.parseHeader(header);
			if (result.error() != null) {
				errors.add(new ValidationError(rule.getName(), result.error()));
			} else {
				return List.of();
			}
		}

		return errors;
	}

	/**
	 * Formats the given file to contain the correct license header.
	 *
	 * @param project the project the file is in
	 * @param logger the logger
	 * @param path the path of the file
	 * @param readComment the read header comment if successful, or {@code null} otherwise
	 * @return {@code true} if files changed, otherwise {@code false}
	 */
	public @Nullable List<String> format(Project project, Logger logger, Path path, @Nullable List<String> readComment) {
		List<String> newHeader = null;

		if (readComment == null) {
			if (YumiLicenserGradlePlugin.DEBUG_MODE) {
				logger.lifecycle("  => Could not find header. Using default rule.", path);
			}

			newHeader = this.format(
					project, path, this.rules.get(0),
					new HeaderRule.ParsedData(Map.of(), Collections.emptySet(), null)
			);
		} else {
			HeaderRule.ParsedData first = null;

			for (var rule : this.rules) {
				var data = rule.parseHeader(readComment);

				if (data.error() == null) {
					if (YumiLicenserGradlePlugin.DEBUG_MODE) {
						logger.lifecycle("  => Found rule in lookup.", path);
					}

					newHeader = this.format(project, path, rule, data);
					break;
				}

				if (first == null) {
					first = data;
				}
			}

			if (newHeader == null) {
				if (YumiLicenserGradlePlugin.DEBUG_MODE) {
					logger.lifecycle("  => Could not find rule in lookup. Using default rule.", path);
				}

				newHeader = this.format(project, path, this.rules.get(0), first);
			}
		}

		if (!newHeader.equals(readComment)) {
			return newHeader;
		} else {
			return null;
		}
	}

	private List<String> format(Project project, Path path, HeaderRule rule, HeaderRule.ParsedData parsed) {
		try {
			int creationYear = rule.getYearSelectionMode().getCreationYear(project, path);
			int lastModifiedYear = rule.getYearSelectionMode().getModificationYear(project, path);
			var context = new HeaderFileContext(path.getFileName().toString(), creationYear, lastModifiedYear);
			return rule.apply(parsed, context);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
