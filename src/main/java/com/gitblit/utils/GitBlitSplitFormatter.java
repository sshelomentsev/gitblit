/*
 * Copyright 2016 gitblit.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitblit.utils;

import static org.eclipse.jgit.lib.Constants.encode;
import static org.eclipse.jgit.lib.Constants.encodeASCII;

import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;

/**
 * Implementation of html snippet generator of a split diff viewer.
 *
 * @author Sergey Shelomentsev <sergey.shelomentsev@xored.com>
 *
 */
public class GitBlitSplitFormatter extends GitBlitDiffFormatter {

	private static final byte[] noNewLine = encodeASCII("\\ No newline at end of file\n"); //$NON-NLS-1$

	private int added;
	private int removed;

	private int context = 3;

	public GitBlitSplitFormatter(String commitId, Repository repository, String path, DiffUtils.BinaryDiffHandler
			handler, int tabLength) {
		super(commitId, repository, path, handler, tabLength);
		added = 0;
		removed = 0;
	}

	private int findCombinedEnd(final List<Edit> edits, final int i) {
		int end = i + 1;
		while (end < edits.size()
				&& (combineA(edits, end) || combineB(edits, end)))
			end++;
		return end - 1;
	}

	private boolean combineA(final List<Edit> e, final int i) {
		return e.get(i).getBeginA() - e.get(i - 1).getEndA() <= 2 * context;
	}

	private boolean combineB(final List<Edit> e, final int i) {
		return e.get(i).getBeginB() - e.get(i - 1).getEndB() <= 2 * context;
	}

	private static boolean end(final Edit edit, final int a, final int b) {
		return edit.getEndA() <= a && edit.getEndB() <= b;
	}

	private static boolean isEndOfLineMissing(final RawText text, final int line) {
		return line + 1 == text.size() && text.isMissingNewlineAtEnd();
	}

	@Override
	public void format(EditList edits, RawText a, RawText b) throws IOException {
		for (int curIdx = 0; curIdx < edits.size();) {
			Edit curEdit = edits.get(curIdx);
			final int endIdx = findCombinedEnd(edits, curIdx);
			final Edit endEdit = edits.get(endIdx);

			int aCur = (int) Math.max(0, (long) curEdit.getBeginA() - context);
			int bCur = (int) Math.max(0, (long) curEdit.getBeginB() - context);
			final int aEnd = (int) Math.min(a.size(), (long) endEdit.getEndA() + context);
			final int bEnd = (int) Math.min(b.size(), (long) endEdit.getEndB() + context);

			writeHunkHeader(aCur, aEnd, bCur, bEnd);

			while (aCur < aEnd || bCur < bEnd) {
				if (aCur < curEdit.getBeginA() || endIdx + 1 < curIdx) {
					writeContextLine(a, aCur);
					if (isEndOfLineMissing(a, aCur))
						os.write(noNewLine);
					aCur++;
					bCur++;
				} else if ((aCur < curEdit.getEndA()) && (bCur < curEdit.getEndB())) {
					writeRemovedLine(a, aCur);
					writeAddedLine(b, bCur);
					aCur++;
					bCur++;
				} else if (aCur < curEdit.getEndA()) {
					writeRemovedLine(a, aCur);
					if (isEndOfLineMissing(a, aCur))
						os.write(noNewLine);
					aCur++;
				} else if (bCur < curEdit.getEndB()) {
					writeAddedLine(b, bCur);
					if (isEndOfLineMissing(b, bCur))
						os.write(noNewLine);
					bCur++;
				}

				if (end(curEdit, aCur, bCur) && ++curIdx < edits.size())
					curEdit = edits.get(curIdx);
			}
		}
	}

	@Override
	protected void writeAddedLine(RawText text, int line) throws IOException {
		if (left >= right && 0 == removed) {
			writeLeftEmptyBlock();
		}
		added++;
		removed = 0;
		os.write(("<th class='diff-line' data-lineno='" + (right++) + "'>").getBytes());
		os.write(("</th>").getBytes());
		os.write("<th class='diff-state diff-state-add'></th>".getBytes());
		os.write("<td class='diff-half-cell add2'>".getBytes());
		os.write(encode(codeLineToHtml(' ', text.getString(line))));
		os.write("</td>".getBytes());
		os.write("</tr>".getBytes());
	}

	@Override
	protected void writeRemovedLine(RawText text, int line) throws IOException {
		removed++;
		added = 0;
		os.write("<tr class='diff-row'>".getBytes());
		os.write(("<th class='diff-line' data-lineno='" + (left++) + "'>").getBytes());
		os.write(("</th>").getBytes());
		os.write("<th class='diff-state diff-state-sub'></th>".getBytes());
		os.write("<td class='diff-half-cell remove2'>".getBytes());
		os.write(encode(codeLineToHtml(' ', text.getString(line))));
		os.write("</td>".getBytes());
	}

	@Override
	protected void writeHunkHeader(int aStartLine, int aEndLine, int bStartLine, int bEndLine) throws IOException {
		System.out.println("hunk header");
		if (nofLinesCurrent++ == 0) {
			handleChange();
			startCurrent = os.size();
		}
		if (!isOff) {
			totalNofLinesCurrent++;
			if (nofLinesCurrent > maxDiffLinesPerFile && maxDiffLinesPerFile > 0) {
				reset();
			} else {
				os.write(("<tr><th class='diff-line' data-lineno='..'></th><th class='diff-line' " +
						"data-lineno='..'></th><td class='hunk_header' colspan='4'>").getBytes());
				os.write('@');
				os.write('@');
				writeRange('-', aStartLine + 1, aEndLine - aStartLine);
				writeRange('+', bStartLine + 1, bEndLine - bStartLine);
				os.write(' ');
				os.write('@');
				os.write('@');
				os.write("</td></tr>\n".getBytes());
			}
		}
		left = aStartLine + 1;
		right = bStartLine + 1;
	}

	private void writeRightEmptyBlock() throws IOException {
		os.write(("<th class='diff-line'><th></th>").getBytes());
		os.write("<td class='diff-half-cell nothing2'>".getBytes());
		os.write("</td></tr>".getBytes());
	}

	private void writeLeftEmptyBlock() throws IOException {
		os.write("<tr class='diff-row'>".getBytes());
		os.write(("<th class='diff-line'></th><th></th>").getBytes());
		os.write("<td class='diff-half-cell nothing2'>".getBytes());
		os.write("</td>".getBytes());
	}

	@Override
	protected void writeContextLine(RawText text, int line) throws IOException {
		if (0 == added && removed > 0) {
			writeRightEmptyBlock();
		}
		added = removed = 0;
		os.write("<tr class='diff-row'>".getBytes());

		os.write(("<th class='diff-line' data-lineno='" + (left++) + "'>").getBytes());
		os.write(("</th>").getBytes());
		os.write("<th class='diff-state'></th>".getBytes());
		os.write("<td class='diff-half-cell context2'>".getBytes());
		os.write(encode(codeLineToHtml(' ', text.getString(line))));
		os.write("</td>".getBytes());

		os.write(("<th class='diff-line' data-lineno='" + (right++) + "'>").getBytes());
		os.write(("</th>").getBytes());
		os.write("<th class='diff-state'></th>".getBytes());
		os.write("<td class='diff-half-cell context2'>".getBytes());
		os.write(encode(codeLineToHtml(' ', text.getString(line))));
		os.write("</td>".getBytes());

		os.write("</tr>".getBytes());
	}
}
