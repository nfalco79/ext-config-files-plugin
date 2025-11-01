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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;

@WithJenkins
public class PyPIServerHelperTest {

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
        PyPIServer privateRegistry = new PyPIServer("server1", "https://private.organization.com/", user.getId());
        PyPIServer officalRegistry = new PyPIServer("server2", "https://registry.npmjs.org/", null);

        FreeStyleBuild build = r.createFreeStyleProject().createExecutable();

        PyPIServerHelper helper = new PyPIServerHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardUsernameCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertThat(resolvedCredentials).isNotEmpty().hasSize(1);

        assertThat(resolvedCredentials.keySet()).contains(privateRegistry.getUrl());
        assertThat(resolvedCredentials.get(privateRegistry.getUrl())).isEqualTo(user);
    }

    @Test
    public void test_server_index() throws Exception {
        PyPIServer privateRegistry = new PyPIServer("server1", "https://private.organization.com/", user.getId());
        PyPIServer officalRegistry = new PyPIServer("server2", "https://registry.npmjs.org/", null);

        FreeStyleBuild build = r.createFreeStyleProject().createExecutable();

        PyPIServerHelper helper = new PyPIServerHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardUsernameCredentials> resolvedCredentials = helper.resolveCredentials(build);

        String pypircContent = IOUtils.toString(getClass().getResourceAsStream("pypirc.config"), StandardCharsets.UTF_8);
        pypircContent = helper.fillRegistry(pypircContent, resolvedCredentials);

        PyPIrc pypirc = new PyPIrc();
        pypirc.from(pypircContent);
        assertThat(pypirc.get("distutils", "index-servers")).contains("server1").contains("server2").contains("pypi").contains("pypitest");
    }

}