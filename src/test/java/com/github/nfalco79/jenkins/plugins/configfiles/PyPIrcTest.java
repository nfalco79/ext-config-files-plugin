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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PyPIrcTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File file;

    @Before
    public void setUp() throws IOException {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream("pypirc.config");
            file = folder.newFile(".pypirc");
            hudson.util.IOUtils.copy(is, file);
        } finally {
            IOUtils.closeQuietly(is);
        }
    }

    @Test
    public void testLoad() throws Exception {
        PyPIrc pypirc = PyPIrc.load(file);
        assertTrue(pypirc.contains("pypi"));
        assertEquals("https://pypi.python.org/pypi", pypirc.get("pypi", "repository"));
    }

    @Test
    public void testAvoidParseError() throws Exception {
        PyPIrc pypirc = PyPIrc.load(file);
        assertFalse(pypirc.contains("browser"));
    }

    @Test
    public void testSave() throws Exception {
        String testServer = "artifactory";
        String testKey = "test";
        String testValue = "value";

        PyPIrc pypirc = PyPIrc.load(file);
        pypirc.set(testServer, testKey, testValue);
        pypirc.save(file);

        // reload content
        pypirc = PyPIrc.load(file);
        assertTrue(pypirc.contains(testServer));
        Assertions.assertThat(pypirc.get(testServer, testKey)).isEqualTo(testValue);
    }

}