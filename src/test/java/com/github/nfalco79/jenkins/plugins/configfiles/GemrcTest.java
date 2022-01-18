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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GemrcTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File file;

    @Before
    public void setUp() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("gemrc.config");
            file = folder.newFile(".gemrc");
            hudson.util.IOUtils.copy(is, file);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    public void testLoad() throws Exception {
        Gemrc gemrc = Gemrc.load(file);
        assertTrue(gemrc.contains(":backtrace"));
        Assertions.assertThat(gemrc.getSources()).isNotNull().isNotEmpty();
        Assertions.assertThat(gemrc.getSources()).contains("https://test:password@gemsource.com/artifactory/api/gems/gems-release/");
    }

    @Test
    public void testAvoidParseError() throws Exception {
        Gemrc gemrc = Gemrc.load(file);
        assertFalse(gemrc.getAsBoolean(":backtrace"));
    }

    @Test
    public void testSave() throws Exception {
        String option = ":backtrace";
        Object optionValue = true;
        URL source = new URL("https://api.gemsource.com/api/gems/gems-release");

        Gemrc gemrc = Gemrc.load(file);
        gemrc.set(option, optionValue);
        gemrc.addSource(source);
        gemrc.save(file);

        // reload content
        gemrc = Gemrc.load(file);
        assertTrue(gemrc.contains(option));
        Assertions.assertThat(gemrc.getAsBoolean(option)).isEqualTo(optionValue);
        Assertions.assertThat(gemrc.getSources()).hasSize(2);
        Assertions.assertThat(gemrc.getSources()).contains(source.toString());
    }

    @Test
    public void testOptions() throws Exception {
        String option = ":backtrace";
        Object optionValue = true;
        URL source = new URL("https://api.gemsource.com/api/gems/gems-release");

        Gemrc gemrc = Gemrc.load(file);
        gemrc.set(option, optionValue);
        gemrc.addSource(source);

        String content = gemrc.toString();
        Assertions.assertThat(content).describedAs("explict start was not set").startsWith("---");
        Assertions.assertThat(content).describedAs("array values style is not set to flow block") //
                .contains("- https://api.gemsource.com/api/gems/gems-release");
    }

}