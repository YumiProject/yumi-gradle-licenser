/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.impl;

import dev.yumi.gradle.licenser.api.rule.HeaderFileContext;
import dev.yumi.gradle.licenser.api.rule.HeaderLine;
import dev.yumi.gradle.licenser.api.rule.HeaderRule;
import dev.yumi.gradle.licenser.api.rule.LicenseYearSelectionMode;
import dev.yumi.gradle.licenser.api.rule.variable.VariableType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Represents the valid license headers for this project.
 *
 * @author LambdAurora
 * @version 2.2.0
 * @since 1.0.0
 */
public final class LicenseHeader implements Serializable {
	private List<HeaderRule> rules;

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
	 * @param rootPath the root directory of the project the path is in
	 * @param projectCreationYear the creation year of the project
	 * @param logger the logger
	 * @param path the path of the file
	 * @param readComment the read header comment if successful, or {@code null} otherwise
	 * @return {@code true} if files changed, otherwise {@code false}
	 */
	public @Nullable List<String> format(
			Path rootPath, int projectCreationYear, LogConsumer logger, Path path, @Nullable List<String> readComment
	) {
		List<String> newHeader = null;

		if (readComment == null) {
			logger.log("  => Could not find header. Using default rule.");

			newHeader = this.format(
					rootPath, projectCreationYear, path, this.rules.get(0),
					new HeaderRule.ParsedData(Map.of(), Collections.emptySet(), null)
			);
		} else {
			HeaderRule.ParsedData first = null;

			for (var rule : this.rules) {
				var data = rule.parseHeader(readComment);

				if (data.error() == null) {
					logger.log("  => Found rule in lookup.");

					newHeader = this.format(rootPath, projectCreationYear, path, rule, data);
					break;
				}

				if (first == null) {
					first = data;
				}
			}

			if (newHeader == null) {
				logger.log("  => Could not find rule in lookup. Using default rule.");

				newHeader = this.format(rootPath, projectCreationYear, path, this.rules.get(0), first);
			}
		}

		if (!newHeader.equals(readComment)) {
			return newHeader;
		} else {
			return null;
		}
	}

	private List<String> format(
			Path rootPath, int projectCreationYear, Path path, HeaderRule rule, HeaderRule.ParsedData parsed
	) {
		try {
			int creationYear = rule.getYearSelectionMode().getCreationYear(rootPath, projectCreationYear, path);
			int lastModifiedYear = rule.getYearSelectionMode().getModificationYear(rootPath, path);
			var context = new HeaderFileContext(path.getFileName().toString(), creationYear, lastModifiedYear);
			return rule.apply(parsed, context);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(this.rules.size());
		for (var rule : this.rules) {
			this.writeRule(out, rule);
		}
	}

	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		int ruleCount = in.readInt();
		this.rules = new ArrayList<>(ruleCount);
		for (int i = 0; i < ruleCount; i++) {
			this.rules.add(this.readRule(in));
		}
	}

	private void writeRule(ObjectOutput out, HeaderRule rule) throws IOException {
		out.writeUTF(rule.getName());

		out.writeObject(rule.getLines());

		var variables = rule.getVariables();
		out.writeInt(variables.size());
		for (var entry : variables.entrySet()) {
			out.writeUTF(entry.getKey());

			for (var knownEntry : VariableType.TYPES.entrySet()) {
				if (knownEntry.getValue() == entry.getValue()) {
					out.writeUTF(knownEntry.getKey());
				}
			}
		}

		out.writeObject(rule.getYearSelectionMode());
	}

	@SuppressWarnings("unchecked")
	private HeaderRule readRule(ObjectInput in) throws IOException, ClassNotFoundException {
		String name = in.readUTF();

		var lines = (List<HeaderLine>) in.readObject();

		int variableCount = in.readInt();
		var variables = new HashMap<String, VariableType<?>>(variableCount);
		for (int i = 0; i < variableCount; i++) {
			String variableName = in.readUTF();
			String variableType = in.readUTF();

			variables.put(variableName, VariableType.TYPES.get(variableType));
		}

		return new HeaderRule(
				name,
				lines,
				variables,
				(LicenseYearSelectionMode) in.readObject()
		);
	}
}
