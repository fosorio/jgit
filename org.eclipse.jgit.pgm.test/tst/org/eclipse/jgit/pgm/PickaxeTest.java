/*
 * Copyright (C) 2017 Francisco Osorio <fosoriog@gmail.com>
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
package org.eclipse.jgit.pgm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.CLIRepositoryTestCase;
import org.eclipse.jgit.pgm.internal.CLIText;
import org.junit.Before;
import org.junit.Test;

public class PickaxeTest extends CLIRepositoryTestCase {
	private Git git;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		git = new Git(db);
	}

	private void commitCommonFiles() throws GitAPIException, IOException {
		commitFiles("commit1", writeTrashFile("file1", "test"));
		commitFiles("commit2", writeTrashFile("file2", "test2"));
		commitFiles("commit3", writeTrashFile("file3", "test3"));
	}

	@Test
	public void testBasicPickaxe() throws Exception {
		// Write all files
		commitCommonFiles();

		String[] result = execute("git log -S test2");

		assertResultEqualsCommits(result, "commit2");
	}

	@Test
	public void testBasicRegex() throws Exception {
		commitCommonFiles();

		// test\d+ should match only numbers (test2 & test3)
		assertResultEqualsCommits(
				execute("git log --pickaxe-regex -S test\\\\d+"), "commit3",
				"commit2");
	}

	@Test
	public void testOptionOrder() throws Exception {
		commitCommonFiles();

		// same test as testBasicRegex, but different order of parameters
		assertResultEqualsCommits(
				execute("git log -S test\\\\d+ --pickaxe-regex"), "commit3",
				"commit2");
	}

	@Test
	public void testEmptyPattern() throws Exception {
		try {
			execute("git log -S");
		} catch (Die e) {
			assertTrue(
					e.getMessage().contains("Option \"-S\" takes an operand"));
			return;
		}
		fail();
	}

	@Test
	public void testInvalidRegexPattern() throws Exception {
		try {
			commitCommonFiles();
			// try to execute with invalid regex pattern (unmatched parens)
			execute("git log -S test\\\\d*( --pickaxe-regex");
		} catch (Die e) {
			assertTrue(e.getMessage().contains(CLIText.get().invalidRegex));
			return;
		}
		fail();
	}

	@Test
	public void testQuotedRegexPattern() throws Exception {
		commitCommonFiles();
		commitFiles("commit4", writeTrashFile("file4", "test4 123"));
		assertResultEqualsCommits(
				execute("git log -S 'test\\d*' --pickaxe-regex"), "commit4",
				"commit3", "commit2", "commit1");

		// We have to double escape the backslashes when unquoted
		assertResultEqualsCommits(
				execute("git log -S test\\\\d* --pickaxe-regex"), "commit4",
				"commit3", "commit2", "commit1");

		assertResultEqualsCommits(
				execute("git log -S 'test\\d* 123' --pickaxe-regex"),
				"commit4");
	}

	@Test
	public void regexTest() throws Exception {
		// Write all files
		commitFiles("commit1", writeTrashFile("file1", "abbcc"));
		commitFiles("commit2", writeTrashFile("file1", "abbccdd"));

		String command = "git log --pickaxe-regex -S ab{2}c{2}";

		// abbcc should match only commit1 (same pattern remains in commit2)
		assertResultEqualsCommits(execute(command), "commit1");

		// abbcc should now match commit3 and commit 1 (pattern was removed in
		// commit 3)
		commitFiles("commit3", writeTrashFile("file1", "abbc"));
		assertResultEqualsCommits(execute(command), "commit3", "commit1");

		// restore abbccdd
		commitFiles("commit4", writeTrashFile("file1", "abbccdd"));
		assertResultEqualsCommits(execute(command), "commit4", "commit3",
				"commit1");

		// test a.*d, should be all commits, except the first one
		assertResultEqualsCommits(execute("git log --pickaxe-regex -S a.*d"),
				"commit4", "commit3", "commit2");

	}

	@Test
	public void testMultilineRegex() throws Exception {
		// Write all files
		commitFiles("commit1", writeTrashFile("file1", "abbcc\nddeeff"));

		assertResultEqualsCommits(
				execute("git log --pickaxe-regex -S (?s)ab{2}.*c{2}.*ff"),
				"commit1");

	}
	private void assertResultEqualsCommits(String[] result, String... commits) {
		assertEquals(toString(commits), toString(getCommitMessages(result)));
	}

	private String[] getCommitMessages(String[] execute) {
		execute = Arrays.copyOfRange(execute, 0, execute.length - 1);
		String[] result = new String[execute.length / 6];
		for (int i = 0; i < execute.length / 6; i++)
			result[i] = execute[6 * i + 4];
		return result;
	}

	private void commitFiles(String message, File... files)
			throws GitAPIException {

		for (File file : files)
			git.add().addFilepattern(file.getName()).call();

		git.commit().setMessage(message).call();
	}



}
