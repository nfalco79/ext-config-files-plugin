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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.github.nfalco79.jenkins.plugins.configfiles.PyPIServer.DescriptorImpl;

import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;

/**
 * Test input form validation.
 *
 * @author Nikolas Falco
 */
@WithJenkins
public class PyPIServerValidatorTest {

    private static JenkinsRule r;

    @BeforeAll
    static void init(JenkinsRule rule) {
        r = rule;
    }

    @Test
    void test_empty_server_url() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.emptyServerURL());
    }

    @Test
    void test_server_url_that_contains_variable() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("${REGISTRY_URL}/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://${SERVER_NAME}/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
        result = descriptor.doCheckUrl("http://acme.com/${CONTEXT_ROOT}");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_empty_server_url_is_ok() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("http://acme.com");
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

    @Test
    void test_server_url_invalid_protocol() throws Exception {
        DescriptorImpl descriptor = new DescriptorImpl();

        FormValidation result = descriptor.doCheckUrl("hpp://acme.com/root");
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.invalidServerURL());
    }

    @Test
    void test_invalid_credentials() throws Exception {
        FreeStyleProject prj = r.createFreeStyleProject();

        String credentialsId = "secret";
        Credentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", "user", "password");
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

        String serverURL = "http://acme.com";
        DescriptorImpl descriptor = (DescriptorImpl) new PyPIServer("server1", serverURL, credentialsId).getDescriptor();
        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);

        result = descriptor.doCheckCredentialsId(prj, "foo", serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.ERROR);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.invalidCredentialsId());
    }

    @Test
    void test_empty_credentials() throws Exception {
        FreeStyleProject prj = mock(FreeStyleProject.class);
        when(prj.hasPermission(isA(Permission.class))).thenReturn(true);

        DescriptorImpl descriptor = mock(DescriptorImpl.class);
        when(descriptor.doCheckCredentialsId(any(Item.class), (String) any(), anyString())).thenCallRealMethod();

        String serverURL = "http://acme.com";

        FormValidation result = descriptor.doCheckCredentialsId(prj, "", serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.emptyCredentialsId());
        result = descriptor.doCheckCredentialsId(prj, null, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.WARNING);
        Assertions.assertThat(result.getMessage()).isEqualTo(Messages.emptyCredentialsId());
    }

    @Test
    void test_credentials_ok() throws Exception {
        FreeStyleProject prj = r.createFreeStyleProject();

        String credentialsId = "secret";
        Credentials credentials = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, credentialsId, "", "user", "password");
        SystemCredentialsProvider.getInstance().getCredentials().add(credentials);

        String serverURL = "http://acme.com";
        DescriptorImpl descriptor = (DescriptorImpl) new PyPIServer("server1", serverURL, credentialsId).getDescriptor();
        FormValidation result = descriptor.doCheckCredentialsId(prj, credentialsId, serverURL);
        Assertions.assertThat(result.kind).isEqualTo(Kind.OK);
    }

}
