package com.gitblit.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.gitblit.models.Metric;
import com.gitblit.tickets.BranchTicketService;

import static org.junit.Assert.*;

public class MetricUtilsMissingRefTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void ignoresTicketHistoryWhenThereIsNoDefaultBranch() throws Exception {
		try (Git git = Git.init().setBare(true).setDirectory(folder.newFolder()).call()) {
			Repository repository = git.getRepository();
			ObjectId id;
			try (ObjectInserter inserter = repository.newObjectInserter()) {
				PersonIdent author = new PersonIdent("Test Author", "test@example.invalid");
				CommitBuilder commit = new CommitBuilder();
				commit.setTreeId(inserter.insert(new TreeFormatter()));
				commit.setAuthor(author);
				commit.setCommitter(author);
				commit.setMessage("Ticket metadata");
				id = inserter.insert(commit);
				inserter.flush();
				RefUpdate ref = repository.updateRef(BranchTicketService.BRANCH);
				ref.setNewObjectId(id);
				assertEquals(RefUpdate.Result.NEW, ref.update());
			}
			assertTrue(JGitUtils.hasCommits(repository));
			assertNull(JGitUtils.getDefaultBranch(repository));

			List<LoggingEvent> events = new ArrayList<>();
			AppenderSkeleton appender = new AppenderSkeleton() {
				@Override
				protected void append(LoggingEvent event) {
					events.add(event);
				}

				@Override
				public void close() {
				}

				@Override
				public boolean requiresLayout() {
					return false;
				}
			};
			Logger logger = Logger.getLogger(MetricUtils.class);
			logger.addAppender(appender);
			try {
				TimeZone utc = TimeZone.getTimeZone("UTC");
				List<Metric> metrics = MetricUtils.getDateMetrics(repository, null, true, null, utc);
				assertEquals(1, metrics.size());
				assertEquals(0.0, metrics.get(0).count, 0.0);
				assertTrue(MetricUtils.getDateMetrics(repository, "refs/heads/missing", false, null, utc).isEmpty());
				assertTrue(MetricUtils.getAuthorMetrics(repository, null, false).isEmpty());

				RefUpdate branch = repository.updateRef("refs/heads/source");
				branch.setNewObjectId(id);
				assertEquals(RefUpdate.Result.NEW, branch.update());
				assertEquals(id, JGitUtils.getDefaultBranch(repository));
				metrics = MetricUtils.getDateMetrics(repository, null, true, null, utc);
				assertEquals(2, metrics.size());
				assertEquals(1.0, metrics.get(0).count, 0.0);
				assertEquals(1, MetricUtils.getAuthorMetrics(repository, null, false).size());
				assertTrue("Unresolved refs logged errors: " + events.size(), events.isEmpty());
			} finally {
				logger.removeAppender(appender);
			}
		}
	}
}
