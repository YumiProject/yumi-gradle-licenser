/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test;

import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

/**
 * A simple unit test for the 'dev.yumi.gradle.licenser' plugin.
 */
class YumiGradleLicenserPluginTest {
	@Test
	void pluginRegistersATask() {
		// Create a test project and apply the plugin
		Project project = ProjectBuilder.builder().build();
		project.getPlugins().apply("dev.yumi.gradle.licenser");
	}
}
