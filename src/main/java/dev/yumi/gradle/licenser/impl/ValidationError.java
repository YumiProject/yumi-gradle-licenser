/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.impl;

import dev.yumi.gradle.licenser.api.rule.HeaderParseException;

/**
 * Represents a validation error of a license header.
 *
 * @param headerRule the rule which produced the error when reading the license header
 * @param error the error
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public record ValidationError(String headerRule, HeaderParseException error) {}
