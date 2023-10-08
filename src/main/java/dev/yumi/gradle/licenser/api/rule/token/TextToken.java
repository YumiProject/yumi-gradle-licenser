/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule.token;

/**
 * Represents a text token in a header rule definition.
 *
 * @param content the context of the text
 * @author LambdAurora
 * @version 1.0.0
 * @since 1.0.0
 */
public record TextToken(String content) implements RuleToken {
}
