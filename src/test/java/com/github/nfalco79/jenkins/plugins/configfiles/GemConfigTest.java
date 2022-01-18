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

import org.assertj.core.api.Assertions;
import org.jenkinsci.lib.configprovider.model.Config;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.github.nfalco79.jenkins.plugins.configfiles.GemConfig;
import com.github.nfalco79.jenkins.plugins.configfiles.GemConfig.GemConfigProvider;

import hudson.model.Descriptor;

public class GemConfigTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void test_load_template() {
        Descriptor<?> descriptor = j.jenkins.getDescriptor(GemConfig.class);
        Assertions.assertThat(descriptor).isNotNull() //
                .describedAs("Unexpected descriptor class").isInstanceOf(GemConfigProvider.class);

        GemConfigProvider provider = (GemConfigProvider) descriptor;
        Config config = provider.newConfig("testId");
        Assertions.assertThat(config).isNotNull() //
                .describedAs("Unexpected config class").isInstanceOf(GemConfig.class);
        Assertions.assertThat(config.content).describedAs("Expected the default template, instead got empty").isNotBlank();
    }

}