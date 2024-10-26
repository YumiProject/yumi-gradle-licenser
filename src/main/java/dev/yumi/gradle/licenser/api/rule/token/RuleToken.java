/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule.token;

import java.io.Serializable;

/**
 * Represents a token in a header rule definition.
 *
 * @author LambdAurora
 * @version 2.0.0
 * @since 1.0.0
 */
public sealed interface RuleToken extends Serializable permits TextToken, VarToken {
}
