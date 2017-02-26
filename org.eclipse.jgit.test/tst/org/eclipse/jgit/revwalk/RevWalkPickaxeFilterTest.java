package org.eclipse.jgit.revwalk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.filter.PickaxeRevFilter;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.junit.Test;

public class RevWalkPickaxeFilterTest extends RevWalkTestCase {

	@Test
	public void testSingleCommit() throws Exception {
		final RevCommit c1 = commit("c1",
				tree(file("test.txt", blob("This is a test"))));

		RevCommit head = c1;
		getTestRepository().update("master", head);

		logCommits();

		Repository repo = getTestRepository().getRepository();
		testPickaxeWalk(repo, head, "test", c1);

	}

	@Test
	public void testPickaxeRevFilter() throws Exception {
		// @formatter:off
		final RevCommit c1 = commit("c1",
				tree(file("test.txt", blob("This is a test"))));

		final RevCommit c2 = commit("c2",
				tree(file("test2.txt", blob("This is a second test"))), c1);

		final RevCommit c3 = commit("c3",
				tree(file("test3.txt", blob("This is a third test"))), c2);

		final RevCommit c4 = commit("c4",
				tree(file("test3.txt", blob("This is a third test")),
						file("test4.txt", blob("This is a another test"))), c3);

		final RevCommit c5 = commit("c5",
				tree(file("test3.txt", blob("This is a fourth test"))), c4);

		final RevCommit c6 = commit("c6",
				tree(file("test3.txt", blob("This is a fourth test2"))), c5);

		final RevCommit c7a = commit("c7a",
				tree(file("test3.txt", blob("This is a fourth test2 branch")),
					 file("test4.txt", blob("abc"))), c6);

		final RevCommit c7b = commit("c7b",
				tree(file("test3.txt", blob("This is a fourth test2 branch")),
				     file("test4.txt", blob("dce"))), c6);

		final RevCommit c8 = commit("c8",
				tree(file("test3.txt", blob("This is a fourth test3"))), c7a, c7b);
		// @formatter:on

		RevCommit head = c8;
		getTestRepository().update("master", head);

		logCommits();

		Repository repo = getTestRepository().getRepository();
		testPickaxeWalk(repo, head, "third", c5, c3);
		testPickaxeWalk(repo, head, "second", c3, c2);
		testPickaxeWalk(repo, head, "fourth", c5);
		testPickaxeWalk(repo, head, "test2", c8, c6);
		testPickaxeWalk(repo, head, "This", c5, c4, c3, c2, c1);
		testPickaxeWalk(repo, head, "test3", c8);
		testPickaxeWalk(repo, head, "This is a fourth", c5);
		testPickaxeWalk(repo, head, "branch", c8, c7b, c7a);
		testPickaxeWalk(repo, head, "abc", c8, c7a);
		testPickaxeWalk(repo, head, "dce", c8, c7b);

	}

	@Test
	public void testRegexMultiline() throws Exception {
		final RevCommit c1 = commit("c1", tree(file("test.txt",
				blob("This is a test\nThis is another line"))));
		getTestRepository().update(Constants.MASTER, c1);
		RevCommit head = c1;

		logCommits();

		Repository repo = getTestRepository().getRepository();
		testPickaxeWalk(repo, head, "(?s)This.*test", c1);
		testPickaxeWalk(repo, head, "(?s)This.*line", c1);
		testPickaxeWalk(repo, head, "(?s)Not This.*line");

	}

	@Test
	public void testLargeReporsitory() throws Exception {
		FileRepository largeRepository = new FileRepository(
				new File("../.git"));
		Ref head = largeRepository.findRef("master");
		System.out.println(head);

		// a RevWalk allows to walk over commits based on some filtering that is
		// defined
		try (RevWalk walk = new RevWalk(largeRepository)) {
			RevCommit commit = walk.parseCommit(head.getObjectId());

			System.out.println("\nCommit-Message: " + commit.getFullMessage());

			testPickaxeWalk(largeRepository, commit, "EWAHCompressedBitmap",
					"3a6ed050a4f550b801e10344dace5e52db3ac609,26012958a35d85639fe44fdbd4690cb58ec3b836,80edcac06f93b78a14df8c8e4c5360528d9582b7,7c5b2761ed0d5a781b6bdb6e5282797bb1151814,9eda23e469491fcf89108d8e439b4b662c813419,070bf8d15f8659cb5f6a039b070b19909dd2f49f,deb853cb691caf55affe1e4fbfad29b670591140,86af34e150ab58d454eb6c13ffe40a3fe94fdb1b,683bd09092e90aef5b7cf963355995d76aefa439,2d46de03bbed4cea807a94de830a80ccb07e27c7,10fe4a54050d1eb9021322993bbee220e31cb2a0,99d404009428ef42658cf1aa78069ee5382f6bd7,288501df011b623db37378100e697a9cf64b8bc2,320a4142ad0e8febf4696446cc3a6346c0708a00,f32b8612433e499090c76ded014dd5e94322b786,dafcb8f6db82b899c917832768f1c240d273190c,3b325917a5c928caadd88a0ec718b1632f088fd5");

			walk.dispose();
		}

	}


	private void testPickaxeWalk(Repository repo, RevCommit head,
			String pattern,
			String commits) throws Exception {
		String[] shas = commits.split(",");
		RevCommit[] revCommits = new RevCommit[shas.length];
		try (RevWalk walk = new RevWalk(repo)) {
			int i = 0;
			for (String sha : shas) {
				RevCommit commit = walk.parseCommit(repo.resolve(sha));
				revCommits[i++] = commit;
			}
			walk.dispose();
		}
		testPickaxeWalk(repo, head, pattern, revCommits);

	}

	private void logCommits()
			throws GitAPIException, NoHeadException, IOException {
		for (RevCommit revCommit : Git.wrap(getTestRepository().getRepository())
				.log().all().call()) {
			System.out.println(formatCommit(revCommit));
		}
	}

	private RevCommit commit(String message, RevTree tree,
			final RevCommit... parents) throws Exception {
		return getTestRepository().commit(1, tree, message, parents);
	}

	private void testPickaxeWalk(final Repository repo, final RevCommit head,
			String pattern,
			final RevCommit... expectedCommits) throws Exception,
			MissingObjectException, IncorrectObjectTypeException, IOException {

		System.out.println("\n\nmatching pattern " + pattern);

		// compareAgainstGitCli(repo, pattern, expectedCommits);

		RevWalk revWalk = new RevWalk(repo);

		RevFilter filter = PickaxeRevFilter.create(pattern, true,
				repo);
		assertNotNull(filter);
		revWalk.setRevFilter(filter);
		revWalk.markStart(revWalk.parseCommit(head));

		for (RevCommit expectedCommit : expectedCommits) {
			RevCommit actualCommit = revWalk.next();
			assertEquals(expectedCommit, actualCommit);
		}
		assertNull(revWalk.next());
	}

	private String formatCommit(RevCommit c) {
		return c.getShortMessage() + " " + c.name();
	}

	public void compareAgainstGitCli(Repository repo, String pattern,
			RevCommit[] expectedCommits) throws IOException {

		Process exec = Runtime.getRuntime()
				.exec("C:\\Users\\fo05509\\.babun\\cygwin\\bin\\git -C "
						+ repo.getDirectory()
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
