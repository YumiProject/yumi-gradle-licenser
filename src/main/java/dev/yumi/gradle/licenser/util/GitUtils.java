/*
 * Copyright 2023 Yumi Project
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package dev.yumi.gradle.licenser.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

/**
 * Provides various Git-related utilities.
 *
 * @version 1.0.0
 * @since 1.0.0
 */
@ApiStatus.Internal
public final class GitUtils {
	private GitUtils() {
		throw new UnsupportedOperationException("GitUtils only contains static definitions.");
	}

	private static @NotNull String getStandardizedPath(@NotNull Path path) {
		String strValue = path.toString();

		if (!path.getFileSystem().getSeparator().equals("/")) {
			strValue = strValue.replace(path.getFileSystem().getSeparator(), "/");
		}

		return strValue;
	}

	private static Git openGit(Project project) throws IOException {
		return Git.open(project.getRootDir());
	}

	private static Path getRepoRoot(Git git) {
		return git.getRepository().getDirectory().toPath().getParent();
	}

	private static @Nullable AbstractTreeIterator prepareTreeParser(Repository repository, String ref) throws IOException {
		Ref head = repository.getRefDatabase().findRef(ref);

		if (head.getObjectId() == null) {
			return null;
		}

		var walk = new RevWalk(repository);
		RevCommit commit = walk.parseCommit(head.getObjectId());
		RevTree tree = walk.parseTree(commit.getTree().getId());
		var oldTreeParser = new CanonicalTreeParser();

		try (ObjectReader oldReader = repository.newObjectReader()) {
			oldTreeParser.reset(oldReader, tree.getId());
		}

		return oldTreeParser;
	}

	/**
	 * Gets the first commit in the repository.
	 *
	 * @param git the Git instance
	 * @return the first commit
	 */
	public static @Nullable RevCommit getFirstCommit(@NotNull Git git) {
		var walker = new RevWalk(git.getRepository());

		RevCommit first = null;
		for (var commit : walker) {
			first = commit;
		}

		return first;
	}

	/**
	 * Gets the latest commit hash of a file.
	 *
	 * @param git the git instance
	 * @param path the file
	 * @return the latest commit of a file, or {@code null} if the file is not present in any commit
	 */
	public static @Nullable RevCommit getLatestCommit(@NotNull Git git, @NotNull Path path) {
		try {
			var pathStr = getStandardizedPath(path);

			var log = git.log();

			if (!pathStr.isEmpty()) {
				log.addPath(pathStr);
			}

			Iterator<RevCommit> iterator = log
					.setMaxCount(1)
					.call()
					.iterator();

			// No commits exist - you will need to create a commit to have a hash
			if (!iterator.hasNext()) {
				return null;
			}

			// We only care about the last commit
			return iterator.next();
		} catch (GitAPIException e) {
			throw new GradleException(
					String.format("Failed to get commit hash of last commit of path %s", path),
					e
			);
		}
	}

	private static int getLatestCommitYear(Git git, Path path) {
		RevCommit latestCommit = getLatestCommit(git, path);

		if (latestCommit != null) {
			PersonIdent authorIdent = latestCommit.getAuthorIdent();
			Instant instant = authorIdent.getWhenAsInstant();
			TimeZone authorTimeZone = authorIdent.getTimeZone();

			return instant.atZone(authorTimeZone.toZoneId()).getYear();
		}

		return Calendar.getInstance().get(Calendar.YEAR);
	}

	/**
	 * Gets the latest modified year of the given path in the Git history.
	 *
	 * @param project the project
	 * @param path the file path to check the latest modified year of
	 * @return the latest modified year
	 */
	public static int getModificationYear(@NotNull Project project, @NotNull Path path) {
		try (var git = openGit(project)) {
			Path repoRoot = getRepoRoot(git);
			path = repoRoot.relativize(path);
			var pathString = getStandardizedPath(path);

			var formatter = new DiffFormatter(System.out);
			formatter.setRepository(git.getRepository());
			AbstractTreeIterator commitTreeIterator = prepareTreeParser(git.getRepository(), Constants.HEAD);

			if (commitTreeIterator == null) {
				return getLatestCommitYear(git, path);
			}

			var workTreeIterator = new FileTreeIterator(git.getRepository());
			List<DiffEntry> diffEntries = formatter.scan(commitTreeIterator, workTreeIterator);

			for (var entry : diffEntries) {
				if (entry.getNewPath().equals(pathString)) {
					return Calendar.getInstance().get(Calendar.YEAR);
				}
			}

			return getLatestCommitYear(git, path);
		} catch (IOException | GradleException e) {
			// ignored
		}

		return Calendar.getInstance().get(Calendar.YEAR);
	}
}
