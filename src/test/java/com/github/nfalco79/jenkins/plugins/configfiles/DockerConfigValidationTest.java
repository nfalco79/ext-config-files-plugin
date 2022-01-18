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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.github.nfalco79.jenkins.plugins.configfiles.DockerConfig;
import com.github.nfalco79.jenkins.plugins.configfiles.DockerRegistry;
import com.github.nfalco79.jenkins.plugins.configfiles.VerifyConfigProviderException;

public class DockerConfigValidationTest {

    @Test
    public void test_new_config() {
        String id = "test_id";
        DockerConfig config = new DockerConfig(id, "", "", "", null);
        assertEquals(id, config.id);
        assertNull(config.name);
        assertNull(config.comment);
        Assertions.assertThat(config.content).isEmpty();
        assertNotNull(config.getRegistries());
    }

    @Test
    public void test_empty_URL() throws Exception {
        DockerRegistry source = new DockerRegistry(null, null);

        DockerConfig config = new DockerConfig("empty_URL", null, null, null, Arrays.asList(source));
        assertThrows(VerifyConfigProviderException.class, () -> config.doVerify());
    }

    @Test
    public void test_no_exception_if_URL_has_variable() throws Exception {
        DockerRegistry source = new DockerRegistry("${URL}", null);

        DockerConfig config = new DockerConfig("no_exception_if_URL_has_variable", null, null, null, Arrays.asList(source));
        config.doVerify();
    }

}