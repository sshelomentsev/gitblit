package com.gitblit.ci.jenkins.model;

import com.gitblit.models.TicketModel;

/**
 * Information about executed build in Jenkins.
 *
 * @author Yaroslav Pankratyev
 */
public class BuildInfo {
    /**
     * Sha1 hash of the last commit in the push that caused build execution.
     */
    private final String commit;
    /**
     * Status of this build.
     * @see com.gitblit.models.TicketModel.CIScore
     */
    private final TicketModel.CIScore buildStatus;
    /**
     * Build URL.
     */
    private final String url;

    public BuildInfo(String commit, TicketModel.CIScore buildStatus, String url) {
        this.commit = commit;
        this.buildStatus = buildStatus;
        this.url = url;
    }

    public String getCommit() {
        return commit;
    }

    public TicketModel.CIScore getBuildStatus() {
        return buildStatus;
    }

    public String getUrl() {
        return url;
    }
}
