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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.HostnameRequirement;

import hudson.Util;
import hudson.model.Run;
import hudson.util.Secret;

/**
 * Helper to fill properly credentials in the the user configuration file.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public final class GemConfigHelper {

    private final Collection<GemSource> sources;

    public GemConfigHelper(List<GemSource> sources) {
        this.sources = new ArrayList<>(sources);
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

        for (GemSource server : sources) {
            String credentialsId = server.getCredentialsId();
            if (credentialsId != null) {

                // create a domain filter based on registry URL
                final URL serverURL = toURL(server.getUrl());
                List<DomainRequirement> domainRequirements = Collections.emptyList();
                if (serverURL != null) {
                    domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(serverURL.getHost()));
                }

                StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernamePasswordCredentials.class, build, domainRequirements);
                if (c != null) {
                    source2credential.put(server.getUrl(), c);
                }
            }
        }
        return source2credential;
    }

    /**
     * Resolves the given API Key credential.
     *
     * @param build a build being run
     * @return map of registry URL - credential
     */
    public String fillApiKey(String gemrcContent, String apiKey, Run<?, ?> build) {
        if (apiKey == null) {
            return gemrcContent;
        }

        Gemrc gemrc = new Gemrc();
        gemrc.from(gemrcContent);

        StandardUsernamePasswordCredentials c = CredentialsProvider.findCredentialById(apiKey, StandardUsernamePasswordCredentials.class, build, Collections.emptyList());
        if (c != null) {
            String usernameColumnPassword = c.getUsername() + ':' + c.getPassword().getPlainText();
            gemrc.set(":rubygems_api_key", "Basic " + Base64.getEncoder().encodeToString(usernameColumnPassword.getBytes(StandardCharsets.UTF_8)));
        }

        return gemrc.toString();
    }

    /**
     * Fill the gemrc user config with the given sources.
     *
     * @param gemrcContent .gemrc user config
     * @param source2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @return the updated content of the {@code gemContent} with the sources
     *         credentials added
     */
    public String fillSources(String gemrcContent, Map<String, StandardUsernamePasswordCredentials> source2Credentials) {
        if (source2Credentials.isEmpty()) {
            return gemrcContent;
        }

        Gemrc gemrc = new Gemrc();
        gemrc.from(gemrcContent);

        for (GemSource source : sources) {
            String url = source.getUrl();
            if (url == null) {
                continue;
            }

            StandardUsernamePasswordCredentials credentials = source2Credentials.get(url);
            if (credentials != null) {
                String authority = credentials.getUsername() + ":" + Secret.toString(credentials.getPassword());

                try {
                    URL baseURL = new URL(url);
                    gemrc.addSource(new URI(baseURL.getProtocol(), authority, baseURL.getHost(), baseURL.getPort(), baseURL.getPath(), baseURL.getQuery(), null).toURL());
                } catch (MalformedURLException e) {
                    // should never happens since the values was already checked
                    throw new IllegalArgumentException(e);
                } catch (URISyntaxException e) {
                    // never happens URL comes from URI that comes from URL
                }
            }

        }

        return gemrc.toString();
    }

    @Nonnull
    private String fixURL(@Nonnull final String registryURL) {
        String url = registryURL;
        if (!url.endsWith("/")) {
            url += "/";
        }
        return url;
    }

    @Nonnull
    public String calculatePrefix(@Nonnull final String registryURL) {
        String trimmedURL = trimSlash(registryURL);

        URL url = toURL(trimmedURL);
        if (url == null) {
            throw new IllegalArgumentException("Invalid url " + registryURL);
        }

        return "//" + trimmedURL.substring((url.getProtocol() + "://").length()) + '/';
    }

    @Nonnull
    public String compose(@Nonnull final String registryPrefix, @Nonnull final String setting) {
        return registryPrefix + ":" + setting;
    }

    @Nullable
    private String trimSlash(@Nullable final String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
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
