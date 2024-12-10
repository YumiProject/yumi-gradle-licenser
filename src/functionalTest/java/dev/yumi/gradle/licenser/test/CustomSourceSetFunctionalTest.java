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
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A functional test about custom source sets for the 'dev.yumi.gradle.licenser' plugin.
 */
public class CustomSourceSetFunctionalTest {
	@TempDir
	File projectDir;

	@Test
	public void canRunTask() throws IOException {
		var runner = new ScenarioRunner("custom_sourceset", projectDir.toPath(), false);
		runner.setup();

		var result = runner.run();

		// Verify the result
		assertTrue(
				result.getOutput().contains("> Task :applyLicenseCustom SKIPPED"),
				"custom source set should be skipped."
		);

		runner.runCheck();
	}
}
