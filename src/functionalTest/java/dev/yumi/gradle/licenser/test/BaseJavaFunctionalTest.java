/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A simple functional test for the 'dev.yumi.gradle.licenser' plugin.
 */
public class BaseJavaFunctionalTest {
	@TempDir
	File projectDir;

	@Test
	public void canRunTask() throws IOException {
		var runner = new ScenarioRunner("base_java", projectDir.toPath());
		runner.setup();

		Path testClassPath = runner.path("src/main/java/test/TestClass.java");
		Path testCrlfCheckClassPath = runner.path("src/main/java/test/TestClassCrlfCheck.java");
		Path testCrlfApplyClassPath = runner.path("src/main/java/test/TestClassCrlfApply.java");
		Path testClassWithOptionalPath = runner.path("src/main/java/test/TestClassWithOptional.java");
		Path testPackageInfoPath = runner.path("src/main/java/test/package-info.java");

		var result = runner.run();

		// Verify the result
		assertTrue(
				result.getOutput().contains("- Updated file " + testClassPath),
				"Missing updated file string in output log."
		);
		assertTrue(
				result.getOutput().contains("- Updated file " + testCrlfApplyClassPath),
				"Missing updated file string in output log."
		);
		assertTrue(
				result.getOutput().contains("- Updated file " + testPackageInfoPath),
				"Missing updated package-info file string in output log."
		);
		assertFalse(
				result.getOutput().contains("- Updated file " + testCrlfCheckClassPath),
				"Found updated Java with CRLF linefeed file string in output log while it's supposed to not change."
		);
		assertFalse(
				result.getOutput().contains("- Updated file " + testClassWithOptionalPath),
				"Found updated Java with optional line file string in output log while it's supposed to not change."
		);
		assertTrue(
				result.getOutput().contains("Updated 3 out of 5 files."),
				"Missing update status string in output log."
		);

		assertTrue(Files.readString(testClassPath).contains("Sample header "));
		assertTrue(Files.readString(testPackageInfoPath).contains("Sample header "));
		String testClassWithOptionalResult = Files.readString(testClassWithOptionalPath);
		assertTrue(testClassWithOptionalResult.contains("Sample header "));
		assertTrue(testClassWithOptionalResult.contains("Optional sample line."));

		assertTrue(
				Files.readString(testCrlfApplyClassPath).contains("\r\n"),
				"TestClassCrlfApply.java is missing CRLF linefeed."
		);

		runner.runCheck();
	}
}
