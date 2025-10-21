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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A functional test about Kotlin for the 'dev.yumi.gradle.licenser' plugin.
 */
public class KotlinFunctionalTest {
	@TempDir
	File projectDir;

	@Test
	public void canRunBase() throws IOException {
		this.run("kotlin/base", "src/main/kotlin/test/TestKotlinFile.kt");
	}

	@Test
	public void canRunMultiplatform() throws IOException {
		this.run("kotlin/multiplatform", "src/commonMain/kotlin/test/TestKotlinFile.kt");
	}

	private void run(String name, String path) throws IOException {
		var runner = new ScenarioRunner(name, projectDir.toPath(), false);
		runner.setup();

		Path testKotlinPath = runner.path(path);

		var result = runner.run();

		// Verify the result
		assertTrue(
				result.getOutput().contains("Updated 1 out of 1 files."),
				"Missing update status string in output log."
		);

		assertEquals("""
						/*
						 * Sample header ${year}.
						 *
						 * File: TestKotlinFile.kt
						 */
						
						@file:JvmName("TestKotlinFile")
						
						package src.main.kotlin.test
						
						fun main(args: Array<String>) {
							println("Hello, world!")
						}
						""".replace("${year}", String.valueOf(LocalDate.now().getYear())),
				Files.readString(testKotlinPath)
		);

		runner.runCheck();
	}
}
