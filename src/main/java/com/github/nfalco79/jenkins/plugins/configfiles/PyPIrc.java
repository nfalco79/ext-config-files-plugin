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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import ca.szc.configparser.Ini;
import ca.szc.configparser.exceptions.IniParserException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * PyPIrc config file parser.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class PyPIrc {
    private static final String UTF_8 = "UTF-8";

    /**
     * Parse the given file and store internally all user settings and
     * comments.
     *
     * @param file a valid pypirc user config file content.
     * @return the instance of parsed user config.
     * @throws IOException in case of I/O failure during file read
     */
    public static PyPIrc load(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("file " + file + " does not exists or is not file");
        }

        Path path = Paths.get(file.getAbsolutePath());
        String content = new String(Files.readAllBytes(path), UTF_8);

        PyPIrc config = new PyPIrc();
        config.from(content);
        return config;
    }

    private Ini ini;

    /**
     * Default constructor.
     */
    public PyPIrc() {
        ini = new Ini();
        ini.setAllowInterpolation(false);
    }

    /**
     * Parse the given content and store internally all user settings and
     * comments.
     *
     * @param content a valid pypirc user config content.
     * @throws IniParserException in case of parse error
     */
    public void from(String content) throws IniParserException {
        if (content == null) {
            return;
        }

        try (Reader reader = new StringReader(content)) {
            ini.read(new BufferedReader(reader));
        } catch (IniParserException e) {
            throw e;
        } catch (IOException e) {
            // should never happens, it write on a string
        }
    }

    @Override
    public String toString() {
        try (Writer writer = new StringWriter()) {
            BufferedWriter bw = new BufferedWriter(writer);
            ini.write(bw);
            bw.flush();
            return writer.toString();
        } catch (IOException e) {
            // should never happens, it write on a string
            return "";
        }
    }

    /**
     * Write the content of user config to a file.
     *
     * @param file the destination file
     * @throws IOException in case of I/O write error
     */
    public void save(File file) throws IOException {
        ini.write(file.toPath(), Charset.forName("UTF-8"));
    }

    /**
     * Returns {@literal true} if this ini contains the given server section.
     *
     * @param section name
     * @return {@literal true} if this config already contains the specified section
     */
    public boolean contains(String section) {
        return ini.getSections().containsKey(section);
    }
    
    /**
     * Creates a new section.
     *
     * @param section name
     */
    public void add(String section) {
        ini.getSections().put(section, new HashMap<String, String>());
    }

    /**
     * Returns {@literal true} if this map contains a user config for the
     * specified key.
     *
     * @param section name
     * @param key user setting whose presence in this config
     * @return {@literal true} if this config already contains the specified key
     */
    public boolean contains(String section, String key) {
        if (!contains(section)) {
            return false;
        }
        return ini.getSections().get(section).containsKey(key);
    }

    /**
     * Create a new section entry.
     *
     * @param section name
     * @param key user config entry key
     * @return the property value
     */
    public String get(String section, String key) {
        if (!contains(section)) {
            return null;
        }
        return ini.getSections().get(section).get(key);
    }

    /**
     * Set the value for the specified property key. If key already present it
     * will be override.
     *
     * @param section name
     * @param key property key
     * @param value property value
     */
    public void set(String section, String key, String value) {
        if (!contains(section)) {
            add(section);
        }
        ini.getSections().get(section).put(key, value);
    }

    /**
     * Set the value for the specified property key. If key already present it
     * will be override.
     *
     * @param section name
     * @param key property key
     * @param value property value
     */
    public void set(String section, String key, boolean value) {
        if (!contains(section)) {
            add(section);
        }
        ini.getSections().get(section).put(key, String.valueOf(value));
    }

    /**
     * Get the value for the specified property key as a boolean.
     *
     * @param section name
     * @param key user config entry key
     * @return a boolean represented by the property value or {@literal null} if
     *         the key doesn't exist or the value associated is empty.
     */
    @SuppressFBWarnings(value = "NP_BOOLEAN_RETURN_NULL", justification = "null is null, the caller will check the value")
    public Boolean getAsBoolean(String section, String key) {
        if (!contains(section)) {
            return null;
        }
        return Boolean.parseBoolean(ini.getSections().get(section).get(key));
    }

    /**
     * Get the value for the specified property key as a number.
     *
     * @param section name
     * @param key user config entry key
     * @return an integer represented by the property value or {@literal null}
     *         if the key doesn't exist or the value associated is empty.
     */
    public Integer getAsNumber(String section, String key) {
        if (!contains(section)) {
            return null;
        }
        return Integer.parseInt(ini.getSections().get(section).get(key));
    }

}