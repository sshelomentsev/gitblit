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

import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;

/**
 * Implementation of html snippet generator of a unified diff viewer.
 *
 * @author Sergey Shelomentsev <sergey.shelomentsev@xored.com>
 *
 */
public class GitBlitUnifiedFormatter extends GitBlitDiffFormatter {

	public GitBlitUnifiedFormatter(String commitId, Repository repository, String path, DiffUtils.BinaryDiffHandler
			handler, int tabLength) {
		super(commitId, repository, path, handler, tabLength);
	}

	@Override
	protected void writeLine(char prefix, RawText text, int cur) throws IOException {
		if (nofLinesCurrent++ == 0) {
			handleChange();
			startCurrent = os.size();
		}
		// update entry diffstat
		currentPath.update(prefix);
		if (isOff) {
			return;
		}
		totalNofLinesCurrent++;
		if (nofLinesCurrent > maxDiffLinesPerFile && maxDiffLinesPerFile > 0) {
			reset();
		} else {
			// output diff
			os.write("<tr>".getBytes());
			switch (prefix) {
				case '+':
					os.write(("<th class='diff-line'></th><th class='diff-line' data-lineno='" + (right++) + "'></th>").getBytes());
					os.write("<th class='diff-state diff-state-add'></th>".getBytes());
					os.write("<td class='diff-cell add2'>".getBytes());
					break;
				case '-':
					os.write(("<th class='diff-line' data-lineno='" + (left++) + "'></th><th class='diff-line'></th>").getBytes());
					os.write("<th class='diff-state diff-state-sub'></th>".getBytes());
					os.write("<td class='diff-cell remove2'>".getBytes());
					break;
				default:
					os.write(("<th class='diff-line' data-lineno='" + (left++) + "'></th><th class='diff-line' data-lineno='" + (right++) + "'></th>").getBytes());
					os.write("<th class='diff-state'></th>".getBytes());
					os.write("<td class='diff-cell context2'>".getBytes());
					break;
			}
			os.write(encode(codeLineToHtml(prefix, text.getString(cur))));
			os.write("</td></tr>\n".getBytes());
		}
	}
}
