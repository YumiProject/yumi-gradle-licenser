/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment;

import org.jspecify.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Manages the different header comment implementations for given file types.
 *
 * @author LambdAurora
 * @version 3.0.0
 * @since 1.0.0
 */
public class HeaderCommentManager implements Serializable {
	private Map<String, HeaderComment> headers = new HashMap<>();

	public HeaderCommentManager() {
		this.register(
				CStyleHeaderComment.INSTANCE,
				// C/++
				"c",
				"cpp",
				"cxx",
				"h",
				"hpp",
				"hxx",

				// Java
				"java",

				// Kotlin
				"kt",
				"kts",

				// Scala
				"scala",

				// Groovy
				"groovy",
				"gradle",

				// Web languages
				"dart", // Dart language
				"js",   // JavaScript
				"jsx",  // JavaScript XML
				"ts",   // TypeScript
				"tsx",  // TypeScript XML

				// Stylesheets
				"css",  // CSS stylesheets
				"less", // Less (Extended CSS)
				"scss", // SCSS (CSS syntax for SASS)
				"styl"  // Stylus (Alternative CSS syntax)
		);

		this.register(
				XmlStyleHeaderComment.INSTANCE,
				// Web markup
				"htm",
				"html",
				"xhtml",

				// Extended HTML
				"svelte",
				"vue",

				// Data formats
				"xml",

				// Image formats
				"svg"
		);
	}

	/**
	 * Registers a header comment implementation for a given extension.
	 *
	 * @param extension the extension to match files for which the given header comment implementation applies
	 * @param headerComment the header comment implementation
	 * @since 3.0.0
	 */
	public void register(String extension, HeaderComment headerComment) {
		this.headers.put(extension, headerComment);
	}

	/**
	 * Registers a header comment implementation for a given file pattern.
	 *
	 * @param extensions the extensions to match files for which the given header comment implementation applies
	 * @param headerComment the header comment implementation
	 * @since 3.0.0
	 */
	public void register(HeaderComment headerComment, String... extensions) {
		for (var extension : extensions) {
			this.headers.put(extension, headerComment);
		}
	}

	/**
	 * Registers a header comment implementation for a given file pattern.
	 *
	 * @param extensions the extensions to match files for which the given header comment implementation applies
	 * @param headerComment the header comment implementation
	 * @since 2.0.0
	 */
	public void register(Set<String> extensions, HeaderComment headerComment) {
		for (var extension : extensions) {
			this.headers.put(extension, headerComment);
		}
	}

	/**
	 * Finds the header comment implementation to use for the given file.
	 *
	 * @param path the file
	 * @return the header comment implementation if a suitable one could be found, or {@code null} otherwise
	 */
	public @Nullable HeaderComment findHeaderComment(Path path) {
		var fileName = path.getFileName().toString();
		int indexOfExtSeparator = fileName.lastIndexOf('.');
		var fileExt = indexOfExtSeparator >= 0 ? fileName.substring(indexOfExtSeparator + 1).toLowerCase() : "";

		for (var entry : this.headers.entrySet()) {
			if (entry.getKey().contains(fileExt)) {
				return entry.getValue();
			}
		}

		return null;
	}

	@Serial
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeInt(this.headers.size());

		for (var entry : this.headers.entrySet()) {
			out.writeUTF(entry.getKey());

			if (entry.getValue() instanceof CStyleHeaderComment) {
				out.writeUTF("c_style");
			} else if (entry.getValue() instanceof XmlStyleHeaderComment) {
				out.writeUTF("xml_style");
			} else {
				out.writeUTF("unknown");
				out.writeObject(entry.getValue());
			}
		}
	}

	@Serial
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		var map = new HashMap<String, HeaderComment>();
		int entries = in.readInt();

		for (int i = 0; i < entries; i++) {
			var extension = in.readUTF();

			HeaderComment comment = switch (in.readUTF()) {
				case "c_style" -> CStyleHeaderComment.INSTANCE;
				case "xml_style" -> XmlStyleHeaderComment.INSTANCE;
				default -> (HeaderComment) in.readObject();
			};

			map.put(extension, comment);
		}

		this.headers = map;
	}
}
