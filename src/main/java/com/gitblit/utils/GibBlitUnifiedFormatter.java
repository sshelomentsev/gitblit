package com.gitblit.utils;

import org.eclipse.jgit.diff.DiffFormatter;

import java.io.OutputStream;

public class GibBlitUnifiedFormatter extends DiffFormatter {
	/**
	 * Create a new formatter with a default level of context.
	 *
	 * @param out
	 * 	the stream the formatter will write line data to. This stream
	 * 	should have buffering arranged by the caller, as many small
	 * 	writes are performed to it.
	 */
	public GibBlitUnifiedFormatter(OutputStream out) {
		super(out);
	}

	//public GitBlitUnifiedFormatter(String commitId, String path, DiffUtils.BinaryDiffHandler)

}
