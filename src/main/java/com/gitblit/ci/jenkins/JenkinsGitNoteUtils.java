package com.gitblit.ci.jenkins;

import com.gitblit.models.TicketModel;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.NoSuchElementException;

/**
 * Utility class to handle git notes and add/read CI-connected information (using JSON data format).
 *
 * @author Yaroslav Pankratyev
 */
public final class JenkinsGitNoteUtils {
    private static final String CI_STATUS = "ciStatus";
    private static final String CI_JOB_URL = "ciJobUrl";

    private JenkinsGitNoteUtils() { }

    public static GitNoteBuilder createBuilder() {
        return new GitNoteBuilder();
    }

    public static TicketModel.CIScore readCiBuildStatus(String note) {
        try {
            JSONObject noteJsonObject = new JSONObject(note);
            Integer ciStatusIntValue = (Integer) noteJsonObject.get(CI_STATUS);
            if (ciStatusIntValue == null) {
                return null;
            }
            return TicketModel.CIScore.fromScore(ciStatusIntValue);
        } catch (NoSuchElementException | ClassCastException | JSONException ignore) {
            return null;
        }
    }

    public static String readCiJobUrl(String note) {
        try {
            JSONObject noteJsonObject = new JSONObject(note);
            return  (String) noteJsonObject.get(CI_JOB_URL);
        } catch (ClassCastException | JSONException ignore) {
            return null;
        }
    }

    public static class GitNoteBuilder {
        private final JSONObject note;

        private GitNoteBuilder() {
            note = new JSONObject();
        }

        public GitNoteBuilder addCiBuildStatus(TicketModel.CIScore buildStatus) {
            note.put(CI_STATUS, buildStatus.getValue());
            return this;
        }

        public GitNoteBuilder addCiJobUrl(String ciJobUrl) {
            note.put(CI_JOB_URL, ciJobUrl);
            return this;
        }

        // utils to manage code line comments can be placed here

        public String build() {
            return note.toString();
        }
    }
}
