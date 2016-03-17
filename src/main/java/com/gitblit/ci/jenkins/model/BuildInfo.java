package com.gitblit.ci.jenkins.model;

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
     * String representation of the build result: SUCCESS, UNSTABLE, FAILURE, NOT_BUILD, ABORTED, IN_PROGRESS.
     */
    private final String result;
    /**
     * Build URL.
     */
    private final String url;

    public BuildInfo(String commit, String result, String url) {
        this.commit = commit;
        this.result = result;
        this.url = url;
    }

    public String getCommit() {
        return commit;
    }

    public String getResult() {
        return result;
    }

    public String getUrl() {
        return url;
    }
}
