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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;

public class GemSourceHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private StandardUsernameCredentials user;

    @Before
    public void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
    }

    @Test
    public void test_registry_credentials_resolution() throws Exception {
        GemSource privateSource = new GemSource("https://private.organization.com/", user.getId());
        GemSource officalSource = new GemSource("https://registry.npmjs.org/", null);

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        GemConfigHelper helper = new GemConfigHelper(Arrays.asList(privateSource, officalSource));
        Map<String, StandardUsernamePasswordCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertFalse(resolvedCredentials.isEmpty());
        assertEquals(1, resolvedCredentials.size());

        assertThat(resolvedCredentials.keySet(), hasItem(privateSource.getUrl()));
        assertThat(resolvedCredentials.get(privateSource.getUrl()), equalTo(user));
    }

    @Test
    public void test_apikey() throws Exception {
        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        GemConfigHelper helper = new GemConfigHelper(Collections.emptyList());
        String gemrc = helper.fillApiKey("---\r\n" +
                ":backtrace: false\r\n" +
                ":bulk_threshold: 1000\r\n" +
                ":update_sources: true\r\n" +
                ":verbose: true", user.getId(), build);
        Assertions.assertThat(gemrc).contains(":rubygems_api_key: Basic " + Base64.encodeBase64String("myuser:mypassword".getBytes()));
    }

}