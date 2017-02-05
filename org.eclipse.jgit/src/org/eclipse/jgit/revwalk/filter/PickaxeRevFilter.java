/*
 * Copyright (C) 2017, Francisco Osorio <fosoriog@gmail.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * This program and the accompanying materials are made available
 * under the terms of the Eclipse Distribution License v1.0 which
 * accompanies this distribution, is reproduced below, and is
 * available at http://www.eclipse.org/org/documents/edl-v10.php
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials provided
 *   with the distribution.
 *
 * - Neither the name of the Eclipse Foundation, Inc. nor the
 *   names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.jgit.revwalk.filter;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.AbbreviatedObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

/**
 * Matches only commits whose diffs don't match in the number of occurrences of
 * the given pattern.
 *
 * @since 4.7
 */
public class PickaxeRevFilter extends RevFilter {
	private String pattern;

	private boolean regex;

	private Pattern regexPattern;

	private Repository repo;

	/**
	 * Create a message filter.
	 * <p>
	 * An optimized substring search may be automatically selected if the
	 * pattern does not contain any regular expression meta-characters.
	 * <p>
	 * The search is performed using a case-insensitive comparison. The
	 * character encoding of the commit message itself is not respected. The
	 * filter matches on raw UTF-8 byte sequences.
	 *
	 * @param pattern
	 *            regular expression pattern to match.
	 * @param regex
	 *            whether the pattern is a simple string or a regex
	 * @param repo
	 *            The parent repository that created this RevWalk
	 * @return a new filter that matches the given expression against the
	 *         message body of the commit.
	 */
	public static RevFilter create(String pattern, boolean regex,
			Repository repo) {
		if (pattern.length() == 0)
			throw new IllegalArgumentException(JGitText.get().cannotMatchOnEmptyString);
		return new PickaxeRevFilter(pattern, regex, repo);
	}

	private PickaxeRevFilter(String pattern, boolean regex, Repository repo) {
		this.pattern = pattern;
		this.regex = regex;
		this.repo = repo;
		if (regex)
			this.regexPattern = Pattern.compile(pattern);
	}

	@Override
	public boolean include(RevWalk walker, RevCommit cmit)
			throws StopWalkException, MissingObjectException,
			IncorrectObjectTypeException, IOException {

		cmit = walker.parseCommit(cmit);

		ObjectReader objectReader = walker.getObjectReader();

		CanonicalTreeParser currentCommitTreeParser = new CanonicalTreeParser(
				null,
				objectReader, cmit.getTree().getId());

		// Root commit
		if (cmit.getParentCount() == 0) {
			System.out.println("Testing root ");
			return findPatternInDiff(new EmptyTreeIterator(),
					currentCommitTreeParser);
		}

		for (RevCommit parentCommit : cmit.getParents()) {
			CanonicalTreeParser parentCommitTreeParser = new CanonicalTreeParser(
					null, objectReader,
					walker.parseCommit(parentCommit).getTree().getId());

			currentCommitTreeParser.reset();

			walker.parseBody(parentCommit);
			// System.out.println("Testing " + cmit.getShortMessage()
			// //$NON-NLS-1$
			// + " against parent " + parentCommit.getShortMessage());

			boolean findPatternInDiffInParent = findPatternInDiff(
					parentCommitTreeParser, currentCommitTreeParser);
			if (findPatternInDiffInParent)
			{
				System.out.println("While testing " + cmit //$NON-NLS-1$
						+ " against parent " + parentCommit + "\n");
				return true;
			}
		}

		return false;
	}

	private boolean findPatternInDiff(
			AbstractTreeIterator parentCommitTreeParser,
			AbstractTreeIterator currentCommitTreeParser)
			throws MissingObjectException, IOException {
		try (Git git = new Git(repo)) {
			List<DiffEntry> diffs = git.diff().setShowNameAndStatusOnly(true)
					.setNewTree(currentCommitTreeParser)
					.setOldTree(parentCommitTreeParser).call();
			for (DiffEntry entry : diffs) {
				if (matchesPickaxe(entry))
					return true;
			}

			return false;
		} catch (GitAPIException e) {
			e.printStackTrace();
			return false;
		}
	}

	private boolean matchesPickaxe(DiffEntry entry)
			throws MissingObjectException, IOException {

		int oldOcurrences = countOcurrences(entry.getOldId());
		int newOcurrences = countOcurrences(entry.getNewId());

		boolean foundMatch = oldOcurrences != newOcurrences;

		if (foundMatch)
			debugMatch(entry, oldOcurrences, newOcurrences);
		return foundMatch;
	}

	private void debugMatch(DiffEntry entry, int oldOcurrences,
			int newOcurrences) {
		System.out.println("\tfor entry " + entry);
		System.out
				.println("\tparent " + entry.getOldPath() + ": "
						+ oldOcurrences);
		System.out
				.println("\tnew: " + entry.getNewPath() + ": " + newOcurrences);
	}

	private int countOcurrences(AbbreviatedObjectId id)
			throws MissingObjectException, IOException {
		if (id.toObjectId().equals(ObjectId.zeroId()))
			return 0;
		ObjectLoader loader = repo.open(id.toObjectId());

		// and then one can the loader to read the file
		String str = new String(loader.getCachedBytes());

		int count = regex ? countPattern(str) : countSubstring(str);

		return count;

	}

	private int countPattern(String str) {
		Matcher matcher = regexPattern.matcher(str);
		int from = 0;
		int count = 0;
		while (matcher.find(from)) {
			count++;
			from = matcher.start() + 1;
		}
		return count;
	}

	private int countSubstring(String str) {
		int lastIndex = 0;
		int count = 0;

		while (lastIndex != -1) {

			lastIndex = str.indexOf(pattern, lastIndex);

			if (lastIndex != -1) {
				count++;
				lastIndex += pattern.length();
			}
		}
		return count;
	}

	@Override
	public RevFilter clone() {
		return null;
	}


}
