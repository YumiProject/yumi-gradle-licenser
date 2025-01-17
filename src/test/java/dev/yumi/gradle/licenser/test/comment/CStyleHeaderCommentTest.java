/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.test.comment;

import dev.yumi.gradle.licenser.api.comment.CStyleHeaderComment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CStyleHeaderCommentTest {
	//region C

	@Test
	void testCHeaderParsing() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/*
				 * Sample License Header
				 *
				 * Yippee
				 */
				
				#include <stdio.h>
				
				void main() {
					printf("Hello world\\n");
				}
				""");

		assertEquals(0, result.start());
		assertEquals(44, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(3, result.existing().size());
		assertEquals("Sample License Header", result.existing().get(0));
		assertEquals("Yippee", result.existing().get(2));
	}

	@Test
	void testCHeaderParsingSingle() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/* Smol */
				#include <stdio.h>
				
				void main() {
					printf("Hello world\\n");
				}
				""");

		assertEquals(0, result.start());
		assertEquals(10, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(1, result.existing().size());
		assertEquals("Smol", result.existing().get(0));
	}

	@Test
	void testCHeaderParsingNone() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				#include <stdio.h>
				
				void main() {
					printf("Hello world\\n");
				}
				""");

		assertEquals(0, result.start());
		assertEquals(0, result.end());
		assertEquals("\n", result.separator());
		assertNull(result.existing(), "Expected no result.");
	}

	//endregion

	//region Java

	@Test
	void testJavaHeaderParsing() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/*
				 * Sample License Header
				 *
				 * Yippee
				 */
				
				package dev.yumi.gradle.licenser.test;
				
				class Test {}
				""");

		assertEquals(0, result.start());
		assertEquals(44, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(3, result.existing().size());
		assertEquals("Sample License Header", result.existing().get(0));
		assertEquals("Yippee", result.existing().get(2));
	}

	@Test
	void testJavaHeaderParsingSingle() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/* Smol */
				package dev.yumi.gradle.licenser.test;
				
				class Test {}
				""");

		assertEquals(0, result.start());
		assertEquals(10, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(1, result.existing().size());
		assertEquals("Smol", result.existing().get(0));
	}

	@Test
	void testJavaHeaderParsingNone() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				package dev.yumi.gradle.licenser.test;
				
				class Test {}
				""");

		assertEquals(0, result.start());
		assertEquals(0, result.end());
		assertEquals("\n", result.separator());
		assertNull(result.existing(), "Expected no result.");
	}

	//endregion

	//region Kotlin

	@Test
	void testKotlinHeaderParsing() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/*
				 * Sample License Header
				 *
				 * Yippee
				 */
				
				@file:JvmName("TestKotlinFile")
				
				package dev.yumi.gradle.licenser.test
				
				fun main(args: Array<String>) {
					println("Hello, world!")
				}
				""");

		assertEquals(0, result.start());
		assertEquals(44, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(3, result.existing().size());
		assertEquals("Sample License Header", result.existing().get(0));
		assertEquals("Yippee", result.existing().get(2));
	}

	@Test
	void testKotlinHeaderParsingSingle() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/* Smol */
				@file:JvmName("TestKotlinFile")
				
				package dev.yumi.gradle.licenser.test
				
				fun main(args: Array<String>) {
					println("Hello, world!")
				}
				""");

		assertEquals(0, result.start());
		assertEquals(10, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(1, result.existing().size());
		assertEquals("Smol", result.existing().get(0));
	}

	@Test
	void testKotlinHeaderParsingNone() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				@file:JvmName("TestKotlinFile")
				
				package dev.yumi.gradle.licenser.test
				
				fun main(args: Array<String>) {
					println("Hello, world!")
				}
				""");

		assertEquals(0, result.start());
		assertEquals(0, result.end());
		assertEquals("\n", result.separator());
		assertNull(result.existing(), "Expected no result.");
	}

	//endregion

	//region Scala

	@Test
	void testScalaHeaderParsing() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/*
				 * Sample License Header
				 *
				 * Yippee
				 */
				
				@main def HelloWorld(args: String*): Unit =
					println("Hello, World!")
				""");

		assertEquals(0, result.start());
		assertEquals(44, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(3, result.existing().size());
		assertEquals("Sample License Header", result.existing().get(0));
		assertEquals("Yippee", result.existing().get(2));
	}

	@Test
	void testScalaHeaderParsingSingle() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				/* Smol */
				@main def HelloWorld(args: String*): Unit =
					println("Hello, World!")
				""");

		assertEquals(0, result.start());
		assertEquals(10, result.end());
		assertEquals("\n", result.separator());
		assertNotNull(result.existing());
		assertEquals(1, result.existing().size());
		assertEquals("Smol", result.existing().get(0));
	}

	@Test
	void testScalaHeaderParsingNone() {
		var result = CStyleHeaderComment.INSTANCE.readHeaderComment("""
				@main def HelloWorld(args: String*): Unit =
					println("Hello, World!")
				""");

		assertEquals(0, result.start());
		assertEquals(0, result.end());
		assertEquals("\n", result.separator());
		assertNull(result.existing(), "Expected no result.");
	}

	//endregion

	//region writing

	@Test
	void testHeaderWriting() {
		assertEquals("""
						/*
						 * Sample License Header
						 *
						 * Yippee
						 */""",
				CStyleHeaderComment.INSTANCE.writeHeaderComment(List.of("Sample License Header", "", "Yippee"), "\n")
		);
	}

	@Test
	void testHeaderWritingSingle() {
		assertEquals("""
						/*
						 * Sample License Header
						 */""",
				CStyleHeaderComment.INSTANCE.writeHeaderComment(List.of("Sample License Header"), "\n")
		);
	}

	//endregion
}
