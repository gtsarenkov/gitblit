/*
 * Copyright 2011 gitblit.com.
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
package com.gitblit.wicket;

import com.gitblit.IStoredSettings;
import com.gitblit.Keys;
import com.gitblit.utils.XssFilter;
import org.apache.wicket.Page;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;
import org.apache.wicket.util.string.StringValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps Gitblit's ordered path parameters while keeping other parameters in the query string.
 */
public class GitblitParamUrlRequestMapper extends MountedMapper {

    public <C extends Page> GitblitParamUrlRequestMapper(IStoredSettings settings,
            XssFilter xssFilter, String mountPath, Class<C> bookmarkablePageClass,
            String[] parameterNames) {
        super(mountPath, bookmarkablePageClass,
                new GitblitPageParametersEncoder(settings, xssFilter, mountPath, parameterNames));
    }

    private static class GitblitPageParametersEncoder extends PageParametersEncoder {
        private static final Logger logger = LoggerFactory.getLogger(GitblitParamUrlRequestMapper.class);

        private final IStoredSettings settings;
        private final XssFilter xssFilter;
        private final String mountPath;
        private final String[] parameterNames;

        GitblitPageParametersEncoder(IStoredSettings settings, XssFilter xssFilter,
                String mountPath, String[] parameterNames) {
            this.settings = settings;
            this.xssFilter = xssFilter;
            this.mountPath = mountPath;
            this.parameterNames = parameterNames.clone();
        }

        @Override
        public Url encodePageParameters(PageParameters parameters) {
            PageParameters remaining = new PageParameters(parameters);
            int lastSpecified = parameterNames.length - 1;
            while (lastSpecified >= 0 && !remaining.getNamedKeys().contains(parameterNames[lastSpecified])) {
                lastSpecified--;
            }

            Url path = new Url();
            char slash = settings.getChar(Keys.web.forwardSlashCharacter, '/');
            for (int i = 0; i <= lastSpecified; i++) {
                String value = remaining.get(parameterNames[i]).toString("");
                path.getSegments().add(slash == '/' ? value : value.replace('/', slash));
                remaining.remove(parameterNames[i]);
            }
            Url extra = super.encodePageParameters(remaining);
            path.getSegments().addAll(extra.getSegments());
            path.getQueryParameters().addAll(extra.getQueryParameters());
            return path;
        }

        @Override
        public PageParameters decodePageParameters(Url url) {
            Url remaining = new Url(url);
            int count = Math.min(parameterNames.length, remaining.getSegments().size());
            remaining.getSegments().subList(0, count).clear();
            PageParameters parameters = super.decodePageParameters(remaining);
            if (parameters == null) {
                parameters = new PageParameters();
            }
            char slash = settings.getChar(Keys.web.forwardSlashCharacter, '/');
            for (int i = 0; i < count; i++) {
                String value = url.getSegments().get(i);
                // A trailing slash in a legacy query URL is not a path value.
                if (value.isEmpty() && i == url.getSegments().size() - 1
                        && parameters.getNamedKeys().contains(parameterNames[i])) {
                    continue;
                }
                parameters.set(parameterNames[i], slash == '/' ? value : value.replace(slash, '/'));
            }

            PageParameters sanitized = new PageParameters(parameters);
            for (String name : parameters.getNamedKeys()) {
                if (name.startsWith("wicket:")) {
                    continue;
                }
                sanitized.remove(name);
                for (StringValue value : parameters.getValues(name)) {
                    String safeValue = xssFilter.none(value.toString());
                    if (!value.toString().equals(safeValue)) {
                        logger.warn("XSS filter triggered on {} URL parameter: {}={}", mountPath, name, value);
                    }
                    sanitized.add(name, safeValue);
                }
            }
            return sanitized;
        }
    }
}
