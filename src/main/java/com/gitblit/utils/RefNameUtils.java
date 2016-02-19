package com.gitblit.utils;

import com.gitblit.Constants;

/**
 * Utility class for manipulating the ref names.
 *
 * @author Yaroslav Pankratyev
 */
public class RefNameUtils {

    /** Extracts the ticket id from the ref name */
    public static long getTicketId(String refName) {
        if (refName.indexOf('%') > -1) {
            refName = refName.substring(0, refName.indexOf('%'));
        }
        if (refName.startsWith(Constants.R_FOR)) {
            String ref = refName.substring(Constants.R_FOR.length());
            try {
                return Long.parseLong(ref);
            } catch (Exception e) {
                // not a number
            }
        } else if (refName.startsWith(Constants.R_TICKET) ||
                refName.startsWith(Constants.R_TICKETS_PATCHSETS)) {
            return getTicketNumber(refName);
        }
        return 0L;
    }

    public static long getTicketNumber(String ref) {
        if (ref.startsWith(Constants.R_TICKETS_PATCHSETS)) {
            // patchset revision

            // strip changes ref
            String p = ref.substring(Constants.R_TICKETS_PATCHSETS.length());
            // strip shard id
            p = p.substring(p.indexOf('/') + 1);
            // strip revision
            p = p.substring(0, p.indexOf('/'));
            // parse ticket number
            return Long.parseLong(p);
        } else if (ref.startsWith(Constants.R_TICKET)) {
            String p = ref.substring(Constants.R_TICKET.length());
            // parse ticket number
            return Long.parseLong(p);
        }
        return 0L;
    }
}
