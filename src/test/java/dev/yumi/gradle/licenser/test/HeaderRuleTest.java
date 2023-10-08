/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import dev.yumi.gradle.licenser.api.rule.HeaderLine;
import dev.yumi.gradle.licenser.api.rule.HeaderRule;
import dev.yumi.gradle.licenser.api.rule.LicenseYearSelectionMode;
import dev.yumi.gradle.licenser.api.rule.token.TextToken;
import dev.yumi.gradle.licenser.api.rule.token.VarToken;
import dev.yumi.gradle.licenser.api.rule.variable.VariableType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HeaderRuleTest {
	@Test
	public void testParseEmpty() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", Collections.emptyList());
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule("Test", Collections.emptyList(), VariableType.DEFAULT_VARIABLES, LicenseYearSelectionMode.PROJECT), rule);
		});
	}

	@Test
	public void testParseSingleSimple() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of("Test header."));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new TextToken("Test header.")
									),
									false
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}

	@Test
	public void testParseSingleVar() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of("${" + VariableType.CREATION_YEAR_VAR_NAME + "}"));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new VarToken(VariableType.CREATION_YEAR_VAR_NAME)
									),
									false
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}

	@Test
	public void testParseEscapedVar() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of("\\${YEAR}"));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new TextToken("\\${YEAR}")
									),
									false
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}


	@Test
	public void testParseSingleVars() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of("${YEAR}${" + VariableType.CREATION_YEAR_VAR_NAME + "}", "#type YEAR YEAR_LENIENT_RANGE"));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new VarToken("YEAR"),
											new VarToken(VariableType.CREATION_YEAR_VAR_NAME)
									),
									false
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}

	@Test
	public void testParseSingle() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of("Test header with ${" + VariableType.CREATION_YEAR_VAR_NAME + "}."));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new TextToken("Test header with "),
											new VarToken(VariableType.CREATION_YEAR_VAR_NAME),
											new TextToken(".")
									),
									false
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}

	@Test
	public void testParseYearSelectionMode() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of(
					"#year_selection file"
			));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.FILE
			), rule);
		});
	}

	@Test
	public void testParseMulti() {
		assertDoesNotThrow(() -> {
			var rule = HeaderRule.parse("Test", List.of(
					"",
					"Test header with ${" + VariableType.CREATION_YEAR_VAR_NAME + "}.",
					"#optional",
					"",
					"Optional text.",
					"#end",
					""
			));
			assertNotNull(rule, "Header rule should not be null.");
			assertEquals(new HeaderRule(
					"Test",
					List.of(
							new HeaderLine(
									List.of(
											new TextToken("Test header with "),
											new VarToken(VariableType.CREATION_YEAR_VAR_NAME),
											new TextToken(".")
									),
									false
							),
							new HeaderLine(
									List.of(),
									true
							),
							new HeaderLine(
									List.of(new TextToken("Optional text.")),
									true
							)
					),
					VariableType.DEFAULT_VARIABLES,
					LicenseYearSelectionMode.PROJECT
			), rule);
		});
	}
}
