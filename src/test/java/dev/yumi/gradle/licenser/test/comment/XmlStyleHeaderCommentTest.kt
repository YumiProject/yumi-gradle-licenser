/*
 * Copyright 2024 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test.comment

import dev.yumi.gradle.licenser.api.comment.XmlStyleHeaderComment
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class XmlStyleHeaderCommentTest {
	@Test
	fun `Parsing with existing header`() {
		val result = XmlStyleHeaderComment.INSTANCE.readHeaderComment(
			"""
			<!--
				Sample License Header

				Yippee
			-->
			
			<!doctype html>
			<html>
				<head>
				</head>
				<body>
				</body>
			</html
		""".trimIndent()
		)

		assertEquals(0, result.start)
		assertEquals(40, result.end)
		assertEquals("\n", result.separator)

		assertNotNull(result.existing)

		assertEquals(3, result.existing?.size)
		assertEquals("Sample License Header", result.existing?.first())
		assertEquals("Yippee", result.existing?.last())

		assert(result.existing != null)
	}

	@Test
	fun `Parsing with missing header`() {
		val result = XmlStyleHeaderComment.INSTANCE.readHeaderComment(
			"""
			<!doctype html>
			<html>
				<head>
				</head>
				<body>
				</body>
			</html
		""".trimIndent()
		)

		assertEquals(0, result.start)
		assertEquals(0, result.end)
		assertEquals("\n", result.separator)

		assertNull(result.existing) { "Expected no result." }
	}

	@Test
	fun `Writing a header`() {
		val expected = """
			<!--
				Sample License Header
				
				Yippee
			-->
		""".trimIndent()

		val result = XmlStyleHeaderComment.INSTANCE.writeHeaderComment(
			listOf(
				"Sample License Header",
				"",
				"Yippee",
			),

			"\n"
		)

		assertEquals(expected, result)
	}
}
