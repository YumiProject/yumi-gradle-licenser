/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.rule;

import dev.yumi.gradle.licenser.api.rule.token.RuleToken;
import dev.yumi.gradle.licenser.api.rule.token.TextToken;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a line of a {@linkplain HeaderRule header rule}.
 *
 * @param tokens the tokens of this line
 * @param optional {@code true} if this line is optional, or {@code false} otherwise
 * @author LambdAurora
 * @version 2.0.0
 * @since 1.0.0
 */
public record HeaderLine(List<RuleToken> tokens, boolean optional) implements Serializable {
	/**
	 * {@return {@code true} if this line is empty, or {@code false} otherwise}
	 */
	public boolean isEmpty() {
		if (this.tokens.isEmpty()) {
			return true;
		} else if (this.tokens.size() == 1) {
			return this.tokens.get(0) instanceof TextToken text && text.content().isBlank();
		} else {
			return false;
		}
	}
}
