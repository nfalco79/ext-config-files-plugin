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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;

public class DockerConfigHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private StandardUsernamePasswordCredentials user;

    @Before
    public void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
    }

    @Test
    public void test_registry_credentials_resolution() throws Exception {
        DockerRegistry privateRegistry = new DockerRegistry("https://private.organization.com/", user.getId());
        DockerRegistry officalRegistry = new DockerRegistry("https://registry.npmjs.org/", null);

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        DockerConfigHelper helper = new DockerConfigHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardUsernamePasswordCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertFalse(resolvedCredentials.isEmpty());
        assertEquals(1, resolvedCredentials.size());

        Assertions.assertThat(resolvedCredentials).containsKey(privateRegistry.getUrl()).containsValue(user);
    }

}