/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import org.gradle.testkit.runner.UnexpectedBuildFailure;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FailOnMissingHeaderCommentHandlerTest {
	@TempDir
	File projectDir;

	@Test
	void doesFailTask() throws IOException {
		var runner = new ScenarioRunner("fail_on_missing_header_comment", projectDir.toPath(), false);
		runner.setup();

		Path testClassPath = runner.path("src/main/java/test/Test.unrecognized");

		var applyError = assertThrows(UnexpectedBuildFailure.class, runner::run);
		assertTrue(
				applyError.getMessage().contains("No header comment handler found for file " + testClassPath + "."),
				"Missing no header comment handler found message."
		);
		var checkError = assertThrows(UnexpectedBuildFailure.class, runner::runCheck);
		assertTrue(
				checkError.getMessage().contains("No header comment handler found for file " + testClassPath + "."),
				"Missing no header comment handler found message."
		);
	}
}
