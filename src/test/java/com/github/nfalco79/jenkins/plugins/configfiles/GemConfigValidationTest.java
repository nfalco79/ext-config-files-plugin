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
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

public class GemConfigValidationTest {

    @Test
    void test_new_config() {
        String id = "test_id";
        GemConfig config = new GemConfig(id, "", "", "", null);
        assertThat(config.id).isEqualTo(id);
        assertThat(config.name).isNull();
        assertThat(config.comment).isNull();
        assertThat(config.content).isNull();
        assertThat(config.getSources()).isNotNull();
    }

    @Test
    void test_empty_URL() throws Exception {
        GemSource source = new GemSource(null, null);

        GemConfig config = new GemConfig("empty_URL", null, null, null, Arrays.asList(source));
        assertThatThrownBy(() -> config.doVerify()).isInstanceOf(VerifyConfigProviderException.class);
    }

    @Test
    void test_no_exception_if_URL_has_variable() throws Exception {
        GemSource source = new GemSource("${URL}", null);

        GemConfig config = new GemConfig("no_exception_if_URL_has_variable", null, null, null, Arrays.asList(source));
        assertThatNoException().isThrownBy(() -> config.doVerify());
    }

}