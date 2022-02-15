/*
 * Copyright 2021 Nikolas Falco
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.github.nfalco79.jenkins.plugins.configfiles;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import org.apache.commons.lang.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.Util;
import hudson.model.Run;
import hudson.util.Secret;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

/**
 * Helper to fill properly credentials in the the user configuration file.
 *
 * @author Nikolas Falco
 * @since 1.0.3
 */
public final class DockerConfigHelper {

    private static final String AUTHS_ELEMENT = "auths";

    private final Collection<DockerRegistry> registries;

    public DockerConfigHelper(List<DockerRegistry> registries) {
        this.registries = new ArrayList<>(registries);
    }

    /**
     * Resolves all source credentials and returns a map paring registry URL
     * to credential.
     *
     * @param build a build being run
     * @return map of registry URL - credential
     */
    public Map<String, StandardUsernamePasswordCredentials> resolveCredentials(Run<?, ?> build) {
        Map<String, StandardUsernamePasswordCredentials> source2credential = new HashMap<>();

        for (DockerRegistry registry : registries) {
            String credentialsId = registry.getCredentialsId();
            if (credentialsId != null) {

                // create a domain filter based on registry URL
                final URL serverURL = toURL(registry.getUrl());
                List<DomainRequirement> domainRequirements = Collections.emptyList();
                if (serverURL != null) {
                    domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(serverURL.getHost()));
                }

                StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, build, domainRequirements);
                if (c != null) {
                    source2credential.put(registry.getUrl(), c);
                }
            }
        }
        return source2credential;
    }

    /**
     * Fill the docker config with the given registries.
     *
     * @param content docker.config content
     * @param registry2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @return the updated content of the {@code gemContent} with the sources
     *         credentials added
     */
    public String fillRegistries(String content, Map<String, StandardUsernamePasswordCredentials> registry2Credentials) {
        if (registry2Credentials.isEmpty()) {
            return content;
        }

        JSON json = StringUtils.isNotBlank(content) ? JSONSerializer.toJSON(content) : new JSONObject();

        JSONObject dockerConfig;
        if (json instanceof JSONObject) {
            dockerConfig = (JSONObject) json;
        } else {
            dockerConfig = new JSONObject();
        }

        JSONObject auths = dockerConfig.optJSONObject(AUTHS_ELEMENT);
        if (auths == null) {
            dockerConfig.put(AUTHS_ELEMENT, new JSONObject());
            auths = dockerConfig.getJSONObject(AUTHS_ELEMENT);
        }

        for (DockerRegistry registry : registries) {
            String url = registry.getUrl();
            if (url == null) {
                continue;
            }

            StandardUsernamePasswordCredentials credentials = registry2Credentials.get(url);
            if (credentials != null) {
                String token = credentials.getUsername() + ":" + Secret.toString(credentials.getPassword());
                token = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));

                JSONObject auth = auths.optJSONObject(url);
                if (auth == null) {
                    auth = new JSONObject();
                }
                auth.put("auth", token);

                auths.put(url, auth);
            }
        }

        return dockerConfig.toString(4);
    }

    @CheckForNull
    private static URL toURL(@Nullable final String url) {
        URL result = null;

        String fixedURL = Util.fixEmptyAndTrim(url);
        if (fixedURL != null) {
            try {
                return new URL(fixedURL);
            } catch (MalformedURLException e) {
                // no filter based on hostname
            }
        }

        return result;
    }

}
