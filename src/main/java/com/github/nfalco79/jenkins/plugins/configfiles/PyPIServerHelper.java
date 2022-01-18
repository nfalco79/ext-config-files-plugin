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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
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
public final class PyPIServerHelper {

    private static final String INDEX_SERVERS = "index-servers";
    private static final String MAIN_SECTION = "distutils";
    private static final String SERVER_URL = "repository";
    private static final String SERVER_USERNAME = "username";
    private static final String SERVER_PASSWORD = "password";

    private final Collection<PyPIServer> servers;

    public PyPIServerHelper(@CheckForNull Collection<PyPIServer> servers) {
        this.servers = servers;
    }

    /**
     * Resolves all server credentials and returns a map paring registry URL
     * to credential.
     *
     * @param build a build being run
     * @return map of registry URL - credential
     */
    public Map<String, StandardUsernameCredentials> resolveCredentials(Run<?, ?> build) {
        Map<String, StandardUsernameCredentials> server2credential = new HashMap<>();
        for (PyPIServer server : servers) {
            String credentialsId = server.getCredentialsId();
            if (credentialsId != null) {

                // create a domain filter based on registry URL
                final URL serverURL = toURL(server.getUrl());
                List<DomainRequirement> domainRequirements = Collections.emptyList();
                if (serverURL != null) {
                    domainRequirements = Collections.<DomainRequirement> singletonList(new HostnameRequirement(serverURL.getHost()));
                }

                StandardUsernameCredentials c = CredentialsProvider.findCredentialById(credentialsId, StandardUsernameCredentials.class, build, domainRequirements);
                if (c != null) {
                    server2credential.put(server.getUrl(), c);
                }
            }
        }
        return server2credential;
    }

    /**
     * Fill the pypirc user config with the given servers.
     *
     * @param pypircContent .pypirc user config
     * @param server2Credentials the credentials to be inserted into the user
     *        config (key: registry URL, value: Jenkins credentials)
     * @return the updated version of the {@code pypircContent} with the registry
     *         credentials added
     * @throws IOException when parse errors occurs
     */
    public String fillRegistry(String pypircContent, Map<String, StandardUsernameCredentials> server2Credentials) throws IOException {
        PyPIrc pypirc = new PyPIrc();
        pypirc.from(pypircContent);

        Set<String> serverIndex = new LinkedHashSet<>();
        for (PyPIServer server : servers) {
            StandardUsernamePasswordCredentials credentials = null;
            if (server2Credentials.containsKey(server.getUrl())) {
                credentials = (StandardUsernamePasswordCredentials) server2Credentials.get(server.getUrl());
            }

            String serverName = server.getName();
            if (!pypirc.contains(serverName)) {
                pypirc.add(serverName);
            }
            serverIndex.add(serverName);
            // add values to the user config file
            pypirc.set(serverName, SERVER_URL, server.getUrl());
            if (credentials != null) {
                pypirc.set(serverName, SERVER_USERNAME, credentials.getUsername());
                pypirc.set(serverName, SERVER_PASSWORD, Secret.toString(credentials.getPassword()));
            }
        }

        if (pypirc.contains(MAIN_SECTION)) {
            serverIndex.addAll(Arrays.asList(pypirc.get(MAIN_SECTION, INDEX_SERVERS).trim().split("(\\s|\\r\\n|\\n)+")));
        } else {
            pypirc.add(MAIN_SECTION);
        }
        pypirc.set(MAIN_SECTION, INDEX_SERVERS, "\n" + String.join("\n", serverIndex));

        return pypirc.toString();
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
