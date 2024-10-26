/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.api.comment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Manages the different header comment implementations for given file types.
 *
 * @author LambdAurora
 * @version 2.0.0
 * @since 1.0.0
 */
public class HeaderCommentManager implements Serializable {
	private Map<Set<String>, HeaderComment> headers = new HashMap<>();

	public HeaderCommentManager() {
		this.register(
				Set.of(
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
				),
				CStyleHeaderComment.INSTANCE
		);

		this.register(
				Set.of(
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
				),
				XmlStyleHeaderComment.INSTANCE
		);
	}

	/**
	 * Registers a header comment implementation for a given file pattern.
	 *
	 * @param extensions the extensions to match files for which the given header comment implementation applies
	 * @param headerComment the header comment implementation
	 * @since 2.0.0
	 */
	public void register(@NotNull Set<String> extensions, @NotNull HeaderComment headerComment) {
		this.headers.put(extensions, headerComment);
	}

	/**
	 * Finds the header comment implementation to use for the given file.
	 *
	 * @param path the file
	 * @return the header comment implementation if a suitable one could be found, or {@code null} otherwise
	 */
	public @Nullable HeaderComment findHeaderComment(@NotNull Path path) {
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
			out.writeInt(entry.getKey().size());
			for (var ext : entry.getKey()) {
				out.writeUTF(ext);
			}

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
		var map = new HashMap<Set<String>, HeaderComment>();
		int entries = in.readInt();

		for (int i = 0; i < entries; i++) {
			int extensionsLength = in.readInt();
			var extensions = new HashSet<String>(extensionsLength);

			for (int j = 0; j < extensionsLength; j++) {
				extensions.add(in.readUTF());
			}

			HeaderComment comment = switch (in.readUTF()) {
				case "c_style" -> CStyleHeaderComment.INSTANCE;
				case "xml_style" -> XmlStyleHeaderComment.INSTANCE;
				default -> (HeaderComment) in.readObject();
			};

			map.put(Set.copyOf(extensions), comment);
		}

		this.headers = map;
	}
}
