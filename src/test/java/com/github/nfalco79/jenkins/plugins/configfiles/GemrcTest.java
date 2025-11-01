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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class GemrcTest {

    @TempDir
    private File tmpFolder;
    private File file;

    @BeforeEach
    void setUp() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("gemrc.config");
            file = new File(tmpFolder, ".gemrc");
            hudson.util.IOUtils.copy(is, file);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    void testLoad() throws Exception {
        Gemrc gemrc = Gemrc.load(file);
        assertThat(gemrc.contains(":backtrace")).isTrue();
        assertThat(gemrc.getSources()).isNotNull().isNotEmpty();
        assertThat(gemrc.getSources()).contains("https://test:password@gemsource.com/artifactory/api/gems/gems-release/");
    }

    @Test
    void testAvoidParseError() throws Exception {
        Gemrc gemrc = Gemrc.load(file);
        assertThat(gemrc.getAsBoolean(":backtrace")).isFalse();
    }

    @Test
    void testSave() throws Exception {
        String option = ":backtrace";
        Object optionValue = true;
        URL source = new URL("https://api.gemsource.com/api/gems/gems-release");

        Gemrc gemrc = Gemrc.load(file);
        gemrc.set(option, optionValue);
        gemrc.addSource(source);
        gemrc.save(file);

        // reload content
        gemrc = Gemrc.load(file);
        assertThat(gemrc.contains(option)).isTrue();
        assertThat(gemrc.getAsBoolean(option)).isEqualTo(optionValue);
        assertThat(gemrc.getSources()).hasSize(2);
        assertThat(gemrc.getSources()).contains(source.toString());
    }

    @Test
    void testOptions() throws Exception {
        String option = ":backtrace";
        Object optionValue = true;
        URL source = new URL("https://api.gemsource.com/api/gems/gems-release");

        Gemrc gemrc = Gemrc.load(file);
        gemrc.set(option, optionValue);
        gemrc.addSource(source);

        String content = gemrc.toString();
        assertThat(content).describedAs("explict start was not set").startsWith("---");
        assertThat(content).describedAs("array values style is not set to flow block") //
                .contains("- https://api.gemsource.com/api/gems/gems-release");
    }

}