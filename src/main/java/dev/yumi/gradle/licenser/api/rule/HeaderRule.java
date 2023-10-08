/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule;

import dev.yumi.gradle.licenser.api.rule.token.RuleToken;
import dev.yumi.gradle.licenser.api.rule.token.TextToken;
import dev.yumi.gradle.licenser.api.rule.token.VarToken;
import dev.yumi.gradle.licenser.api.rule.variable.VariableType;
import dev.yumi.gradle.licenser.util.Utils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

/**
 * Represents a header rule which describes how a header should look like.
 *
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public class HeaderRule {
	private final String name;
	private final List<HeaderLine> lines;
	private final Map<String, VariableType<?>> variables;
	private final LicenseYearSelectionMode yearSelectionMode;

	public HeaderRule(
			@NotNull String name,
			@NotNull List<HeaderLine> lines,
			@NotNull Map<String, VariableType<?>> variables,
			@NotNull LicenseYearSelectionMode yearSelectionMode
	) {
		this.name = name;
		this.lines = lines;
		this.variables = variables;
		this.yearSelectionMode = yearSelectionMode;
	}

	/**
	 * {@return the name of this header rule}
	 */
	@Contract(pure = true)
	public @NotNull String getName() {
		return this.name;
	}

	/**
	 * {@return a view of the lines of this header}
	 */
	@Contract(pure = true)
	public @UnmodifiableView @NotNull List<HeaderLine> getLines() {
		return Collections.unmodifiableList(this.lines);
	}

	/**
	 * {@return the year selection mode}
	 */
	@Contract(pure = true)
	public @NotNull LicenseYearSelectionMode getYearSelectionMode() {
		return this.yearSelectionMode;
	}

	/**
	 * Parses the given header according to the current rules, may throw an exception if the header is not valid.
	 *
	 * @param header the header to check
	 * @return parsed data, contain the successfully parsed variables, and the error if parsing failed
	 */
	public @NotNull ParsedData parseHeader(@NotNull List<String> header) {
		var variableValues = new HashMap<String, Object>();
		var presentOptionalLines = new HashSet<Integer>();
		int headerLineIndex = 0, ruleLineIndex = 0;

		for (; headerLineIndex < header.size(); headerLineIndex++) {
			String headerLine = header.get(headerLineIndex);

			if (ruleLineIndex >= this.lines.size()) {
				return new ParsedData(variableValues, presentOptionalLines, new HeaderParseException(headerLineIndex, "There is unexpected extra header lines."));
			}

			HeaderLine ruleLine = this.lines.get(ruleLineIndex);
			String error;
			while ((error = this.parseLine(headerLine, ruleLine, variableValues)) != null) {
				if (ruleLine.optional()) { // If the line is optional, attempts to check the next line.
					ruleLine = this.lines.get(++ruleLineIndex);
				} else {
					return new ParsedData(variableValues, presentOptionalLines, new HeaderParseException(headerLineIndex, error));
				}
			}

			if (ruleLine.optional()) {
				presentOptionalLines.add(ruleLineIndex);
			}

			ruleLineIndex++;
		}

		return new ParsedData(variableValues, presentOptionalLines, null);
	}

	private @Nullable String parseLine(
			@NotNull String headerLine,
			@NotNull HeaderLine currentLine,
			@NotNull Map<String, Object> variablesMap
	) {
		int currentIndex = 0;

		for (var token : currentLine.tokens()) {
			if (token instanceof TextToken textToken) {
				String text = textToken.content();
				int theoreticalEnd = currentIndex + text.length();

				if (theoreticalEnd > headerLine.length()) {
					return "Header is cut short, stopped at "
							+ headerLine.length()
							+ " instead of "
							+ theoreticalEnd
							+ ".";
				}

				String toCheck = headerLine.substring(currentIndex, theoreticalEnd);

				if (!text.equals(toCheck)) {
					return "Text differs at " + currentIndex + ", got \"" + toCheck + "\", expected \"" + text + "\".";
				}

				currentIndex = currentIndex + text.length();
			} else if (token instanceof VarToken varToken) {
				var type = this.variables.get(varToken.variable());
				var result = type.parseVar(headerLine, currentIndex);

				if (result.isEmpty()) {
					return "Failed to parse variable \"" + varToken.variable() + "\" at " + currentIndex + ".";
				}

				var old = variablesMap.put(varToken.variable(), result.get().data());
				if (old != null && !old.equals(result.get().data())) {
					return "Diverging variable values for \"" + varToken.variable() + "\".";
				}

				currentIndex = result.get().end();
			}
		}

		return null;
	}

	/**
	 * Applies this header rule to the provided parsed data to create a valid up-to-date header comment.
	 *
	 * @param data the data parsed by attempting to read the header comment
	 * @param context the context of the file to update
	 * @return the updated header comment
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public @NotNull List<String> apply(@NotNull ParsedData data, @NotNull HeaderFileContext context) {
		var result = new ArrayList<String>();

		for (int i = 0; i < this.lines.size(); i++) {
			var line = this.lines.get(i);

			if (!line.optional() || data.presentOptionalLines.contains(i)) {
				var builder = new StringBuilder();

				for (var token : line.tokens()) {
					if (token instanceof TextToken textToken) {
						builder.append(textToken.content());
					} else if (token instanceof VarToken varToken) {
						var type = (VariableType) this.variables.get(varToken.variable());
						var previous = data.variables.get(varToken.variable());

						var newValue = type.getUpToDate(context, previous);

						builder.append(type.getAsString(newValue));
					}
				}

				result.add(builder.toString());
			}
		}

		Utils.trimLines(result, String::isEmpty);

		return result;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;
		HeaderRule that = (HeaderRule) o;
		return Objects.equals(this.lines, that.lines);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.lines);
	}

	@Override
	public String toString() {
		return "HeaderRule{" +
				"lines=" + this.lines +
				'}';
	}

	/**
	 * Parses the header rule from the given header rule text.
	 *
	 * @param name the name of the header rule to parse
	 * @param raw the raw header rule text
	 * @return the parsed header rule
	 * @throws HeaderParseException if parsing the header rule fails
	 */
	public static @NotNull HeaderRule parse(@NotNull String name, @NotNull List<String> raw) throws HeaderParseException {
		List<HeaderLine> parsed = new ArrayList<>();
		var variables = new HashMap<String, VariableType<?>>();
		var yearSelectionMode = LicenseYearSelectionMode.PROJECT;

		boolean optionalMode = false;
		for (int i = 0; i < raw.size(); i++) {
			String line = raw.get(i);

			if (line.startsWith("#")) {
				// Instruction for special behavior.
				String[] instruction = line.substring(1).trim().split("\\s+");

				if (instruction.length == 0) {
					throw new HeaderParseException(i, "No valid instructions could be found.");
				}

				switch (instruction[0]) {
					case "optional":
						optionalMode = true;
						break;
					case "end":
						optionalMode = false;
						break;
					case "type":
						if (instruction.length != 3) {
							throw new HeaderParseException(i, "Invalid type instruction. Expected variable name and type.");
						}

						String variableName = instruction[1];
						String variableTypeRaw = instruction[2];

						var variableType = VariableType.TYPES.get(variableTypeRaw);

						if (variableType == null) {
							throw new HeaderParseException(
									i,
									"Invalid variable type \"" + variableTypeRaw + "\" for variable \"" + variableName + "\"."
							);
						}

						variables.put(variableName, variableType);

						break;
					case "year_selection":
						if (instruction.length != 2) {
							throw new HeaderParseException(i, "Invalid year selection instruction. Expected selection mode (project or file).");
						}

						String modeName = instruction[1];
						var mode = LicenseYearSelectionMode.byName(modeName.toUpperCase());

						if (mode == null) {
							throw new HeaderParseException(
									i,
									"Invalid year selection mode \"" + modeName + "\"."
							);
						}

						yearSelectionMode = mode;

						break;
					default:
						throw new HeaderParseException(i, "Unknown instruction: \"" + instruction[0] + "\".");
				}
			} else {
				parsed.add(parseLine(line, optionalMode));
			}
		}

		// Trim lines.
		Utils.trimLines(parsed, HeaderLine::isEmpty);

		variables.putAll(VariableType.DEFAULT_VARIABLES);
		Set<String> undeclaredVariables = new HashSet<>();

		for (var line : parsed) {
			for (var token : line.tokens()) {
				if (token instanceof VarToken varToken) {
					if (!variables.containsKey(varToken.variable())) {
						undeclaredVariables.add(varToken.variable());
					}
				}
			}
		}

		if (!undeclaredVariables.isEmpty()) {
			throw new HeaderParseException(
					0,
					"Undeclared variables found: " + String.join(", ", undeclaredVariables) + "."
			);
		}

		return new HeaderRule(name, parsed, variables, yearSelectionMode);
	}


	/**
	 * Parses out the tokens of a line of the license header.
	 *
	 * @param line the line to parse
	 * @param optional {@code true} if the line is optional, or {@code false} otherwise
	 * @return the parsed line
	 */
	private static @NotNull HeaderLine parseLine(@NotNull String line, boolean optional) {
		List<RuleToken> tokens = new ArrayList<>();

		int lastLandmark = 0;
		boolean backslash = false;

		for (int i = 0; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '$' && !backslash && Utils.matchCharAt(line, i + 1, '{')) {
				String variable = readVar(line, i + 2);

				if (variable != null) {
					if (lastLandmark != i) {
						tokens.add(new TextToken(line.substring(lastLandmark, i)));
					}

					tokens.add(new VarToken(variable));
					i += variable.length() + 2;
					lastLandmark = i + 1;
				}
			} else if (c == '\\') {
				backslash = !backslash;
			} else {
				backslash = false;
			}
		}

		if (lastLandmark < line.length()) {
			tokens.add(new TextToken(line.substring(lastLandmark)));
		}

		return new HeaderLine(tokens, optional);
	}

	/**
	 * Attempts to read the variable at the given index in the line.
	 *
	 * @param line the line to read the variable from
	 * @param start the index where the variable name starts
	 * @return the variable name, or {@code null} if no variable could be parsed
	 */
	private static @Nullable String readVar(@NotNull String line, int start) {
		int end = start;

		for (int i = start; i < line.length(); i++) {
			char c = line.charAt(i);

			if (c == '_' || (c >= '0' && c <= '9') || (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) {
				continue;
			} else if (c == '}') {
				// END.
				end = i;
				break;
			} else return null;
		}

		if (end != start) {
			return line.substring(start, end);
		} else {
			return null;
		}
	}

	/**
	 * Represents the data parsed by reading a header comment using a given header rule.
	 *
	 * @param variables the parsed variable values
	 * @param presentOptionalLines the indices of the optional lines that were present
	 * @param error an error if the parsing failed, or {@code null} otherwise
	 */
	public record ParsedData(
			Map<String, ?> variables,
			Set<Integer> presentOptionalLines,
			@Nullable HeaderParseException error
	) {}
}
