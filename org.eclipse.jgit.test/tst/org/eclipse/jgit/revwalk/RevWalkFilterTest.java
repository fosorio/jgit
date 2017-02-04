/*
 * Copyright (C) 2009-2010, Google Inc.
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

package org.eclipse.jgit.revwalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.StopWalkException;
import org.eclipse.jgit.revwalk.filter.AndRevFilter;
import org.eclipse.jgit.revwalk.filter.CommitTimeRevFilter;
import org.eclipse.jgit.revwalk.filter.NotRevFilter;
import org.eclipse.jgit.revwalk.filter.OrRevFilter;
import org.eclipse.jgit.revwalk.filter.PickaxeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.junit.Test;

public class RevWalkFilterTest extends RevWalkTestCase {
	private static final MyAll MY_ALL = new MyAll();

	@Test
	public void testFilter_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(RevFilter.ALL);
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_Negate_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(RevFilter.ALL.negate());
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NOT_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(NotRevFilter.create(RevFilter.ALL));
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(RevFilter.NONE);
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NOT_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(NotRevFilter.create(RevFilter.NONE));
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_ALL_And_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(AndRevFilter.create(RevFilter.ALL, RevFilter.NONE));
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NONE_And_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(AndRevFilter.create(RevFilter.NONE, RevFilter.ALL));
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_ALL_Or_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(OrRevFilter.create(RevFilter.ALL, RevFilter.NONE));
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NONE_Or_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(OrRevFilter.create(RevFilter.NONE, RevFilter.ALL));
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_MY_ALL_And_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(AndRevFilter.create(MY_ALL, RevFilter.NONE));
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NONE_And_MY_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(AndRevFilter.create(RevFilter.NONE, MY_ALL));
		markStart(c);
		assertNull(rw.next());
	}

	@Test
	public void testFilter_MY_ALL_Or_NONE() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(OrRevFilter.create(MY_ALL, RevFilter.NONE));
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NONE_Or_MY_ALL() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c = commit(b);

		rw.setRevFilter(OrRevFilter.create(RevFilter.NONE, MY_ALL));
		markStart(c);
		assertCommit(c, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testFilter_NO_MERGES() throws Exception {
		final RevCommit a = commit();
		final RevCommit b = commit(a);
		final RevCommit c1 = commit(b);
		final RevCommit c2 = commit(b);
		final RevCommit d = commit(c1, c2);
		final RevCommit e = commit(d);

		rw.setRevFilter(RevFilter.NO_MERGES);
		markStart(e);
		assertCommit(e, rw.next());
		assertCommit(c2, rw.next());
		assertCommit(c1, rw.next());
		assertCommit(b, rw.next());
		assertCommit(a, rw.next());
		assertNull(rw.next());
	}

	@Test
	public void testCommitTimeRevFilter() throws Exception {
		final RevCommit a = commit();
		tick(100);

		final RevCommit b = commit(a);
		tick(100);

		Date since = getDate();
		final RevCommit c1 = commit(b);
		tick(100);

		final RevCommit c2 = commit(b);
		tick(100);

		Date until = getDate();
		final RevCommit d = commit(c1, c2);
		tick(100);

		final RevCommit e = commit(d);

		{
			RevFilter after = CommitTimeRevFilter.after(since);
			assertNotNull(after);
			rw.setRevFilter(after);
			markStart(e);
			assertCommit(e, rw.next());
			assertCommit(d, rw.next());
			assertCommit(c2, rw.next());
			assertCommit(c1, rw.next());
			assertNull(rw.next());
		}

		{
			RevFilter before = CommitTimeRevFilter.before(until);
			assertNotNull(before);
			rw.reset();
			rw.setRevFilter(before);
			markStart(e);
			assertCommit(c2, rw.next());
			assertCommit(c1, rw.next());
			assertCommit(b, rw.next());
			assertCommit(a, rw.next());
			assertNull(rw.next());
		}

		{
			RevFilter between = CommitTimeRevFilter.between(since, until);
			assertNotNull(between);
			rw.reset();
			rw.setRevFilter(between);
			markStart(e);
			assertCommit(c2, rw.next());
			assertCommit(c1, rw.next());
			assertNull(rw.next());
		}
	}

	@Test
	public void testPickaxeRevFilter() throws Exception {
		// @formatter:off
		final RevCommit c1 = commit(
				tree(file("test.txt", blob("This is a test"))));
		System.out.println("c1 " + c1);


		final RevCommit c2 = commit(
				tree(file("test2.txt", blob("This is a second test"))), c1);
		System.out.println("c2 " + c2);

		final RevCommit c3 = commit(
				tree(file("test3.txt", blob("This is a third test"))), c2);
		System.out.println("c3 " + c3);

		final RevCommit c4 = commit(
				tree(file("test3.txt", blob("This is a third test")),
						file("test4.txt", blob("This is a another test"))), c3);
		System.out.println("c4 " + c4);

		final RevCommit c5 = commit(
				tree(file("test3.txt", blob("This is a fourth test"))), c4);
		System.out.println("c5 " + c5);

		final RevCommit c6 = commit(
				tree(file("test3.txt", blob("This is a fourth test2"))), c5);
		System.out.println("c6 " + c6);

		final RevCommit c7a = commit(
				tree(file("test3.txt", blob("This is a fourth test2 branch")),
					 file("test4.txt", blob("abc"))), c6);
		System.out.println("c7a " + c7a);

		final RevCommit c7b = commit(
				tree(file("test3.txt", blob("This is a fourth test2 branch")),
				     file("test4.txt", blob("dce"))), c6);
		System.out.println("c7b " + c7b);

		final RevCommit c8 = commit(
				tree(file("test3.txt", blob("This is a fourth test3"))), c7a, c7b);
		System.out.println("c8 " + c8);
		// @formatter:on

		RevCommit head = c8;
		getTestRepository().update("master", head);

		testPickaxeWalk(head, "third", c5, c3);
		testPickaxeWalk(head, "second", c3, c2);
		testPickaxeWalk(head, "fourth", c5);
		testPickaxeWalk(head, "test2", c8, c6);
		testPickaxeWalk(head, "This", c5, c4, c3, c2, c1);
		testPickaxeWalk(head, "test3", c8);
		testPickaxeWalk(head, "This is a fourth", c5);
		testPickaxeWalk(head, "branch", c8, c7b, c7a);
		testPickaxeWalk(head, "abc", c8, c7a);
		testPickaxeWalk(head, "dce", c8, c7b);


	}

	private void testPickaxeWalk(final RevCommit head, String pattern,
			final RevCommit... expectedCommits)
			throws Exception, MissingObjectException,
			IncorrectObjectTypeException, IOException {
		System.out.println("\n\nmatching pattern " + pattern);
		for (RevCommit c : expectedCommits) {
			System.out.println(c.name());
		}

		compareAgainstGitCli(pattern, expectedCommits);

		rw.reset();

		RevFilter filter = PickaxeRevFilter.create(pattern, false,
				getTestRepository().getRepository());
			assertNotNull(filter);
			rw.setRevFilter(filter);
			markStart(head);

		for (RevCommit commit : expectedCommits) {
			System.out.println(commit.getId());
			assertCommit(commit, rw.next());
		}
			assertNull(rw.next());
	}

	private void compareAgainstGitCli(String pattern,
			RevCommit[] expectedCommits) throws IOException {

		Process exec = Runtime.getRuntime()
				.exec("C:\\Users\\fo05509\\.babun\\cygwin\\bin\\git -C "
						+ getTestRepository().getRepository().getDirectory()
						+ "\\..\\"
						+ " log --oneline --no-abbrev -m -S'" + pattern + "'");

		List<String> commitsCliSha1 = new BufferedReader(
				new InputStreamReader(exec.getInputStream())).lines()
						.map(s -> s.split(" ")[0]).distinct()
						.collect(Collectors.toList());
		List<String> expectedCommitsSha1 = Arrays.stream(expectedCommits)
				.map(c -> c.name())
				.collect(Collectors.toList());
		assertEquals(expectedCommitsSha1, commitsCliSha1);

	}

	private static class MyAll extends RevFilter {
		@Override
		public RevFilter clone() {
			return this;
		}

		@Override
		public boolean include(RevWalk walker, RevCommit cmit)
				throws StopWalkException, MissingObjectException,
				IncorrectObjectTypeException, IOException {
			return true;
		}

		@Override
		public boolean requiresCommitBody() {
			return false;
		}
	}
}
