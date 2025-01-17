/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment

private const val COMMENT_START = "<!--"
private const val COMMENT_END = "-->"

/**
 * [HeaderComment] implementation for XML-style comments.
 *
 * @author gdude2002
 * @version 2.1.1
 * @since 1.2.0
 */
public open class XmlStyleHeaderComment protected constructor() : HeaderComment {
	override fun readHeaderComment(source: String): HeaderComment.Result {
		val separator = this.extractLineSeparator(source)

		// Find the start of the comment block.
		val firstBlockStart = source.indices.first { index -> !Character.isWhitespace(source[index]) }

		// Find the start of the comment by its opening characters
		val start = source.indexOf(COMMENT_START)

		if (start != firstBlockStart) {
			// If the comment doesn't open on the first character of the block, something fishy is going on.
			return HeaderComment.Result(0, 0, null, separator)
		}

		// We're now officially in the first comment block.

		// Find the last character of the comment, including the closing characters.
		val end = source.indexOf(COMMENT_END) + COMMENT_END.length

		if (start < 0 || end < 0) {
			// If we can't find the start or end of the block, there's no licence header – bail out.
			return HeaderComment.Result(0, 0, null, separator)
		}

		// Grab the licence header comment, and split it into lines.
		val result: MutableList<String> = source.substring(start, end).split(separator).toMutableList()

		// Remove the first and last lines, as those are simply comment start/end characters, and not the licence text.
		result.removeFirst()
		result.removeLast()

		// Remove any indents from the licence header text, and return the result.
		return HeaderComment.Result(start, end, result.map { it.trimIndent() }, separator)
	}

	override fun writeHeaderComment(header: List<String>, separator: String): String =
		buildString {  // Use a string builder to generate the licence header.
			append("$COMMENT_START$separator")

			header.forEach {
				append("\t$it$separator")
			}

			append(COMMENT_END)
		}

	public companion object {
		/** Instance of this header comment type. **/
		@JvmField  // Otherwise this would be `XmlStyleHeaderComment.Companion.getINSTANCE`
		public val INSTANCE: XmlStyleHeaderComment = XmlStyleHeaderComment()
	}
}
