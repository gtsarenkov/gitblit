/*
 * Copyright (c) 2013 by syntevo GmbH. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of syntevo GmbH nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.syntevo.bugtraq;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.Config;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static java.nio.charset.StandardCharsets.UTF_8;

public final class BugtraqConfig {

	// Constants ==============================================================

	private static final String DOT_GIT_BUGTRAQ = ".gitbugtraq";
	private static final String DOT_TGITCONFIG = ".tgitconfig";

	private static final String BUGTRAQ = "bugtraq";

	private static final String URL = "url";
	private static final String ENABLED = "enabled";
	private static final String LOG_REGEX = "logregex";
	private static final String LOG_FILTERREGEX = "logfilterregex";
	private static final String LOG_LINKREGEX = "loglinkregex";
	private static final String LOG_LINKTEXT = "loglinktext";
	private static final String PROJECTS = "projects";

	// Static =================================================================

	@Nullable
	public static BugtraqConfig read(@NotNull Repository repository) throws IOException, ConfigInvalidException {
		Config baseConfig = getBaseConfig(repository, DOT_GIT_BUGTRAQ);
		if (baseConfig == null) {
			baseConfig = getBaseConfig(repository, DOT_TGITCONFIG);
		}

		final Set<String> allNames = new HashSet<String>();
		final Config config;
		try {
			config = repository.getConfig();
		}
		catch (RuntimeException ex) {
			final Throwable cause = ex.getCause();
			if (cause instanceof IOException) {
				throw (IOException)cause;
			}
			throw ex;
		}

		if (getString(null, URL, config, baseConfig) != null) {
			allNames.add(null);
		}
		else {
			allNames.addAll(config.getSubsections(BUGTRAQ));
			if (baseConfig != null) {
				allNames.addAll(baseConfig.getSubsections(BUGTRAQ));
			}
		}

		final List<BugtraqConfigEntry> entries = new ArrayList<>();
		for (String name : allNames) {
			final String url = getString(name, URL, config, baseConfig);
			if (url == null) {
				continue;
			}

			final String enabled = getString(name, ENABLED, config, baseConfig);
			if (enabled != null && !"true".equals(enabled)) {
				continue;
			}

			String idRegex = getString(name, LOG_REGEX, config, baseConfig);
			if (idRegex == null) {
				return null;
			}

			String filterRegex = getString(name, LOG_FILTERREGEX, config, baseConfig);
			final String linkRegex = getString(name, LOG_LINKREGEX, config, baseConfig);
			if (filterRegex == null && linkRegex == null) {
				final String[] split = idRegex.split("\n", Integer.MAX_VALUE);
				if (split.length == 2) {
					// Compatibility with TortoiseGit
					filterRegex = split[0];
					idRegex = split[1];
				}
				else {
					// Backwards compatibility with specification version < 0.3
					final List<String> logIdRegexs = new ArrayList<String>();
					for (int index = 1; index < Integer.MAX_VALUE; index++) {
						final String logIdRegexN = getString(name, LOG_REGEX + index, config, baseConfig);
						if (logIdRegexN == null) {
							break;
						}

						logIdRegexs.add(logIdRegexN);
					}

					if (logIdRegexs.size() > 1) {
						throw new ConfigInvalidException("More than three " + LOG_REGEX + " entries found. This is not supported anymore since bugtraq version 0.3, use " + LOG_FILTERREGEX + " and " + LOG_LINKREGEX + " instead.");
					}
					else if (logIdRegexs.size() == 1) {
						filterRegex = idRegex;
						idRegex = logIdRegexs.get(0);
					}
				}
			}

			final String projectsList = getString(name, PROJECTS, config, baseConfig);
			final List<String> projects;
			if (projectsList != null) {
				projects = new ArrayList<>();

				final StringTokenizer tokenizer = new StringTokenizer(projectsList, ",", false);
				while (tokenizer.hasMoreTokens()) {
					projects.add(tokenizer.nextToken().trim());
				}

				if (projects.isEmpty()) {
					throw new ConfigInvalidException("'" + name + ".projects' must specify at least one project or be not present at all.");
				}
			}
			else {
				projects = null;
			}

			final String linkText = getString(name, LOG_LINKTEXT, config, baseConfig);
			entries.add(new BugtraqConfigEntry(url, idRegex, linkRegex, filterRegex, linkText, projects));
		}

		if (entries.isEmpty()) {
			return null;
		}

		return new BugtraqConfig(entries);
	}

	// Fields =================================================================

	@NotNull
	private final List<BugtraqConfigEntry> entries;

	// Setup ==================================================================

	BugtraqConfig(@NotNull List<BugtraqConfigEntry> entries) {
		this.entries = entries;
	}

	// Accessing ==============================================================

	@NotNull
	public List<BugtraqConfigEntry> getEntries() {
		return Collections.unmodifiableList(entries);
	}

	// Utils ==================================================================

	@Nullable
	private static Config getBaseConfig(@NotNull Repository repository, @NotNull String configFileName) throws IOException, ConfigInvalidException {
		final Config baseConfig;
		if (repository.isBare()) {
			// read bugtraq config directly from the repository
			String content = null;
			RevWalk rw = new RevWalk(repository);
			TreeWalk tw = new TreeWalk(repository);
			tw.setFilter(PathFilterGroup.createFromStrings(configFileName));
			try {
				final Ref ref = repository.findRef(Constants.HEAD);
				if (ref == null) {
					return null;
				}

				ObjectId headId = ref.getTarget().getObjectId();
				if (headId == null || ObjectId.zeroId().equals(headId)) {
					return null;
				}

				RevCommit commit = rw.parseCommit(headId);
				RevTree tree = commit.getTree();
				tw.reset(tree);
				while (tw.next()) {
					ObjectId entid = tw.getObjectId(0);
					FileMode entmode = tw.getFileMode(0);
					if (FileMode.REGULAR_FILE == entmode) {
						ObjectLoader ldr = repository.open(entid, Constants.OBJ_BLOB);
						content = new String(ldr.getCachedBytes(), guessEncoding(commit));
						break;
					}
				}
			}
			finally {
				rw.dispose();
				tw.close();
			}

			if (content == null) {
				// config not found
				baseConfig = null;
			}
			else {
				// parse the config
				Config config = new Config();
				config.fromText(content);
				baseConfig = config;
			}
		}
		else {
			// read bugtraq config from work tree
			final File baseFile = new File(repository.getWorkTree(), configFileName);
			if (baseFile.isFile()) {
				FileBasedConfig fileConfig = new FileBasedConfig(baseFile, repository.getFS());
				fileConfig.load();
				baseConfig = fileConfig;
			}
			else {
				baseConfig = null;
			}
		}
		return baseConfig;
	}

	@NotNull
	private static Charset guessEncoding(RevCommit commit) {
		try {
			return commit.getEncoding();
		} catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
			return UTF_8;
		}
	}

	@Nullable
	private static String getString(@Nullable String subsection, @NotNull String key, @NotNull Config config, @Nullable Config baseConfig) {
		final String value = config.getString(BUGTRAQ, subsection, key);
		if (value != null) {
			return trimMaybeNull(value);
		}

		if (baseConfig != null) {
			return trimMaybeNull(baseConfig.getString(BUGTRAQ, subsection, key));
		}

		return value;
	}

	@Nullable
	private static String trimMaybeNull(@Nullable String string) {
		if (string == null) {
			return null;
		}

		string = string.trim();
		if (string.length() == 0) {
			return null;
		}

		return string;
	}
}