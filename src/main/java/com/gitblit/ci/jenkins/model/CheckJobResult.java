package com.gitblit.ci.jenkins.model;

/**
 * Result of the check job existence operation.
 *
 * @author Yaroslav Pankratyev
 */
public enum CheckJobResult {
    Ok(200), Forbidden(403), NotFound(404);

    CheckJobResult(int value) {
        this.value = value;
    }

    private final int value;
}
