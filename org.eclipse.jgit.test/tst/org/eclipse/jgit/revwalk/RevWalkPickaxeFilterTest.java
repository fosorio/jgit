package org.eclipse.jgit.revwalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.revwalk.filter.PickaxeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.junit.Test;

public class RevWalkPickaxeFilterTest extends RevWalkFilterTest {

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
			final RevCommit... expectedCommits) throws Exception,
			MissingObjectException, IncorrectObjectTypeException, IOException {
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
						+ "\\..\\" + " log --oneline --no-abbrev -m -S'"
						+ pattern + "'");

		List<String> commitsCliSha1 = new BufferedReader(
				new InputStreamReader(exec.getInputStream())).lines()
						.map(s -> s.split(" ")[0]).distinct()
						.collect(Collectors.toList());
		List<String> expectedCommitsSha1 = Arrays.stream(expectedCommits)
				.map(c -> c.name()).collect(Collectors.toList());
		assertEquals(expectedCommitsSha1, commitsCliSha1);

	}

}
