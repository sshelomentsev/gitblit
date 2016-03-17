package com.gitblit.ci.jenkins;

import java.io.IOException;

/**
 * Thrown to indicate that there are some problems while working with Jenkins.
 *
 * @author Yaroslav Pankratyev
 */
public class JenkinsException extends Exception {

    public JenkinsException(String message) {
        super(message);
    }

    public JenkinsException(Throwable cause) {
        super(cause);
    }

    public JenkinsException(String message, Throwable cause) {
        super(message, cause);
    }
}
