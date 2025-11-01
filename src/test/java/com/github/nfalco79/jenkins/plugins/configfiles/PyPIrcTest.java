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

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class PyPIrcTest {

    @TempDir
    private File tmpFolder;
    private File file;

    @BeforeEach
    void setUp() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("pypirc.config");
            file = new File(tmpFolder, ".pypirc");
            hudson.util.IOUtils.copy(is, file);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    void testLoad() throws Exception {
        PyPIrc pypirc = PyPIrc.load(file);
        assertThat(pypirc.contains("pypi")).isTrue();
        assertThat(pypirc.get("pypi", "repository")).isEqualTo("https://pypi.python.org/pypi");
    }

    @Test
    void testAvoidParseError() throws Exception {
        PyPIrc pypirc = PyPIrc.load(file);
        assertThat(pypirc.contains("browser")).isFalse();
    }

    @Test
    void testSave() throws Exception {
        String testServer = "artifactory";
        String testKey = "test";
        String testValue = "value";

        PyPIrc pypirc = PyPIrc.load(file);
        pypirc.set(testServer, testKey, testValue);
        pypirc.save(file);

        // reload content
        pypirc = PyPIrc.load(file);
        assertThat(pypirc.contains(testServer)).isTrue();
        assertThat(pypirc.get(testServer, testKey)).isEqualTo(testValue);
    }

}