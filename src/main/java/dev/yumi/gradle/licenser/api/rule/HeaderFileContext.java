/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule;

/**
 * Represents the context of a file for which the license header is to be updated.
 *
 * @param fileName the name of the file to update
 * @param creationYear the creation year
 * @param lastModifiedYear the last modified year
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public record HeaderFileContext(String fileName, int creationYear, int lastModifiedYear) {
}
