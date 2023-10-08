/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A simple functional test for the 'dev.yumi.gradle.licenser' plugin.
 */
public class YumiGradleLicenserPluginFunctionalTest {
	@TempDir
	File projectDir;

	private Path path(String path) {
		return this.projectDir.toPath().resolve(path).normalize();
	}

	private Path copy(String pathStr) throws IOException {
		Path destinationPath = this.path(pathStr);
		Files.createDirectories(destinationPath.getParent());
		Files.copy(
				Objects.requireNonNull(YumiGradleLicenserPluginFunctionalTest.class.getResourceAsStream("/" + pathStr)),
				destinationPath
		);
		return destinationPath.toAbsolutePath();
	}

	private File getSettingsFile() {
		return new File(this.projectDir, "settings.gradle");
	}

	@Test
	public void canRunTask() throws IOException {
		this.writeString(this.getSettingsFile(), "");
		this.copy("build.gradle");
		this.copy("HEADER");
		this.copy("src/custom/java/test/TestClass2.java");
		Path testClassPath = this.copy("src/main/java/test/TestClass.java");
		Path testCrlfCheckClassPath = this.copy("src/main/java/test/TestClassCrlfCheck.java");
		Path testCrlfApplyClassPath = this.copy("src/main/java/test/TestClassCrlfApply.java");
		Path testClassWithOptionalPath = this.copy("src/main/java/test/TestClassWithOptional.java");
		Path testPackageInfoPath = this.copy("src/main/java/test/package-info.java");
		Path testKotlinPath = this.copy("src/main/kotlin/test/TestKotlinFile.kt");

		// Run the build
		var runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("applyLicenses", "--stacktrace");
		runner.withProjectDir(projectDir);
		BuildResult result = runner.build();

		// Verify the result
		assertTrue(result.getOutput().contains("- Updated file " + testClassPath), "Missing updated file string in output log.");
		assertTrue(result.getOutput().contains("- Updated file " + testCrlfApplyClassPath), "Missing updated file string in output log.");
		assertTrue(result.getOutput().contains("- Updated file " + testPackageInfoPath), "Missing updated package-info file string in output log.");
		assertTrue(result.getOutput().contains("- Updated file " + testKotlinPath), "Missing updated Kotlin file string in output log.");
		assertFalse(result.getOutput().contains("- Updated file " + testCrlfCheckClassPath),
				"Found updated Java with CRLF linefeed file string in output log while it's supposed to not change."
		);
		assertFalse(result.getOutput().contains("- Updated file " + testClassWithOptionalPath),
				"Found updated Java with optional line file string in output log while it's supposed to not change."
		);
		assertTrue(result.getOutput().contains("Updated 4 out of 6 files."), "Missing update status string in output log.");
		assertTrue(result.getOutput().contains("> Task :applyLicenseCustom SKIPPED"), "custom source set should be skipped.");

		assertTrue(Files.readString(testClassPath).contains("Sample header "));
		assertTrue(Files.readString(testPackageInfoPath).contains("Sample header "));
		String testClassWithOptionalResult = Files.readString(testClassWithOptionalPath);
		assertTrue(testClassWithOptionalResult.contains("Sample header "));
		assertTrue(testClassWithOptionalResult.contains("Optional sample line."));

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
				Files.readString(testKotlinPath));

		assertTrue(Files.readString(testCrlfApplyClassPath).contains("\r\n"), "TestClassCrlfApply.java is missing CRLF linefeed.");

		runner = GradleRunner.create();
		runner.forwardOutput();
		runner.withPluginClasspath();
		runner.withArguments("checkLicenses", "--stacktrace");
		runner.withProjectDir(projectDir);
		runner.build();
	}

	private void writeString(File file, String string) throws IOException {
		try (var writer = new FileWriter(file)) {
			writer.write(string);
		}
	}
}