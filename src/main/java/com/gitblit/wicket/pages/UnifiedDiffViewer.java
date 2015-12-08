package com.gitblit.wicket.pages;

import com.gitblit.wicket.CacheControl;
import com.gitblit.wicket.WicketUtils;
import com.gitblit.wicket.panels.CommitHeaderPanel;
import com.gitblit.wicket.panels.LinkPanel;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.util.ArrayList;
import java.util.List;

@CacheControl(CacheControl.LastModified.BOOT)
public class UnifiedDiffViewer extends RepositoryPage {

	public UnifiedDiffViewer(PageParameters params) {
		super(params);

		final Repository r = getRepository();
		final RevCommit commit = getCommit();

		List<String> parents = new ArrayList<String>();
		if (commit.getParentCount() > 0) {
			for (RevCommit parent : commit.getParents()) {
				parents.add(parent.name());
			}
		}

		// commit page links
		if (parents.size() == 0) {
			add(new Label("parentLink", getString("gb.none")));
		} else {
			add(new LinkPanel("parentLink", null, parents.get(0).substring(0, 8),
				CommitDiffPage.class, newCommitParameter(parents.get(0))));
		}
		add(new BookmarkablePageLink<Void>("patchLink", PatchPage.class,
			WicketUtils.newObjectParameter(repositoryName, objectId)));
		add(new BookmarkablePageLink<Void>("commitLink", CommitPage.class,
			WicketUtils.newObjectParameter(repositoryName, objectId)));

		add(new CommitHeaderPanel("commitHeader", repositoryName, commit));

		addFullText("fullMessage", commit.getFullMessage());
	}


	@Override
	protected String getPageName() {
		return getString("gb.unifiedcommitdiff");
	}

	@Override
	protected boolean isCommitPage() {
		return true;
	}
}
