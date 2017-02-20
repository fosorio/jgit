/*
 * Copyright (C) 2012, 2015 François Rey <eclipse.org_@_francois_._rey_._name>
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

import java.io.File;
import java.util.Arrays;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.CLIRepositoryTestCase;
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

	@Test
	public void basicTest() throws Exception {
		// Write all files
		commitFiles("commit1", writeTrashFile("file1", "test"));
		commitFiles("commit2", writeTrashFile("file2", "test2"));
		commitFiles("commit3", writeTrashFile("file3", "test3"));

		String[] result = execute("git log -S test2");

		assertResultEqualsCommits(result, "commit2");
	}

	@Test
	public void regexTest() throws Exception {
		// Write all files
		commitFiles("commit1", writeTrashFile("file1", "test"));
		commitFiles("commit2", writeTrashFile("file2", "test2"));
		commitFiles("commit3", writeTrashFile("file3", "test3"));

		String[] result = execute("git log --pickaxe-regex -S test\\\\d+");

		assertResultEqualsCommits(result, "commit3", "commit2");
	}

	private void assertResultEqualsCommits(String[] result, String... commits) {
		assertEquals(toString(getCommitMessages(result)),
				toString(commits));
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
