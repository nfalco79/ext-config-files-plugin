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

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Docker.config file parser.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class Docker {
    private static final String UTF_8 = "UTF-8";

    /**
     * Parse the given file and store internally all user settings and comments.
     *
     * @param file a valid gemrc user config file content.
     * @return the instance of parsed user config.
     * @throws IOException in case of I/O failure during file read
     */
    public static Docker load(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("file is null");
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("file " + file + " does not exists or is not file");
        }

        Path path = Paths.get(file.getAbsolutePath());
        String content = new String(Files.readAllBytes(path), UTF_8);

        Docker config = new Docker();
        config.from(content);
        return config;
    }

    private Yaml yaml;
    private Map<String, Object> context = new LinkedHashMap<>();

    /**
     * Default constructor.
     */
    public Docker() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(FlowStyle.BLOCK);
        options.setExplicitStart(true);

        yaml = new Yaml(new Representer(), options);
    }

    /**
     * Parse the given content and store internally all user settings and
     * comments.
     *
     * @param content a valid pypirc user config content.
     */
    public void from(String content) {
        if (StringUtils.isBlank(content)) {
            return;
        }
        context = yaml.load(content);
    }

    @Override
    public String toString() {
        return yaml.dump(context);
    }

    /**
     * Write the content of user config to a file.
     *
     * @param file the destination file
     * @throws IOException in case of I/O write error
     */
    public void save(File file) throws IOException {
        try (Writer writer = new FileWriterWithEncoding(file, UTF_8)) {
            yaml.dump(context, writer);
        }
    }

    /**
     * Returns {@code true} if this config contains the given option.
     *
     * @param option name
     * @return {@code true} if this config already contains the specified
     *         section
     */
    public boolean contains(String option) {
        return context.containsKey(option);
    }

    /**
     * Add a new source.
     *
     * @param source url
     */
    public void addSource(URL source) {
        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) context.get(":sources");
        if (sources == null) {
            sources = new LinkedList<>();
        }
        sources.add(source.toString());
        context.put(":sources", sources);
    }

    /**
     * Returns all sources.
     *
     * @return a list of gem source url
     */
    public List<String> getSources() {
        @SuppressWarnings("unchecked")
        List<String> sources = (List<String>) context.get(":sources");
        if (sources != null) {
            return sources;
        }
        return Collections.emptyList();
    }

    /**
     * Returns the value for the specified option as string.
     *
     * @param option name
     * @return the value for this option.
     */
    public String get(String option) {
        return (String) context.get(option);
    }

    /**
     * Returns the value for the specified option as a boolean.
     *
     * @param option name
     * @return a boolean represented by the property value or {@code null} if
     *         the key doesn't exist or the value associated is empty.
     */
    public Boolean getAsBoolean(String option) {
        Object value = context.get(option);
        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return (Boolean) value;
    }

    /**
     * Returns the value for the specified option as a number.
     *
     * @param option name
     * @return an integer represented by the property value or {@code null}
     *         if the key doesn't exist or the value associated is empty.
     */
    public Integer getAsNumber(String option) {
        Object value = context.get(option);
        if (value instanceof String) {
            return Integer.valueOf((String) value);
        }
        return (Integer) value;
    }

    /**
     * Sets the value for the specified option.
     *
     * @param option name
     * @param value to set
     * @return the previous value associated with {@code key}, or {@code null}
     *         if there was no mapping for {@code key}.
     */
    public Object set(String option, Object value) {
        return context.put(option, value);
    }

}