/*
 * Copyright 2021 Nikolas Falco
 *
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;

@WithJenkins
public class GemSourceHelperTest {

    private static JenkinsRule r;

    @BeforeAll
    static void init(JenkinsRule rule) {
        r = rule;
    }

    private StandardUsernameCredentials user;

    @BeforeEach
    void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        CredentialsStore store = CredentialsProvider.lookupStores(r.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
    }

    @Test
    void test_registry_credentials_resolution() throws Exception {
        GemSource privateSource = new GemSource("https://private.organization.com/", user.getId());
        GemSource officalSource = new GemSource("https://registry.npmjs.org/", null);

        FreeStyleBuild build = r.createFreeStyleProject().createExecutable();

        GemConfigHelper helper = new GemConfigHelper(Arrays.asList(privateSource, officalSource));
        Map<String, StandardUsernamePasswordCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertThat(resolvedCredentials).isNotEmpty().hasSize(1);

        assertThat(resolvedCredentials.keySet()).contains(privateSource.getUrl());
        assertThat(resolvedCredentials.get(privateSource.getUrl())).isEqualTo(user);
    }

    @Test
    void test_apikey() throws Exception {
        FreeStyleBuild build = r.createFreeStyleProject().createExecutable();

        GemConfigHelper helper = new GemConfigHelper(Collections.emptyList());
        String gemrc = helper.fillApiKey("---\r\n" +
                ":backtrace: false\r\n" +
                ":bulk_threshold: 1000\r\n" +
                ":update_sources: true\r\n" +
                ":verbose: true", user.getId(), build);
        assertThat(gemrc).contains(":rubygems_api_key: Basic " + Base64.encodeBase64String("myuser:mypassword".getBytes()));
    }

}