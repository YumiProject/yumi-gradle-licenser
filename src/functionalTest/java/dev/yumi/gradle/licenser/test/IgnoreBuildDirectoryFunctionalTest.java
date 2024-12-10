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
 * A functional test which tests the proper exclusion of build directories even though files may be included in a source set.
 */
public class IgnoreBuildDirectoryFunctionalTest {
	@TempDir
	File projectDir;

	@Test
	public void canRunTask() throws IOException {
		var runner = new ScenarioRunner("ignore_build_directory", projectDir.toPath(), false);
		runner.setup();

		runner.runCheck();
	}
}
