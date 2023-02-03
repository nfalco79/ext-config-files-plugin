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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.json.JsonConfig;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * A config/provider to handle the special case of a docker config file.
 *
 * @author Nikolas Falco
 * @since 1.0.3
 */
public class DockerConfig extends JsonConfig {
    private static final long serialVersionUID = 1L;

    private final List<DockerRegistry> registries;

    @DataBoundConstructor
    public DockerConfig(@NonNull String id, String name, String comment, String content, List<DockerRegistry> registries) {
        super(id, Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(comment), content == null ? "" : content);
        this.registries = registries == null ? new ArrayList<>(3) : registries;
    }


    public List<DockerRegistry> getRegistries() {
        return registries;
    }

    /**
     * Perform a validation of the configuration.
     * <p>
     * If validation pass then no {@link VerifyConfigProviderException} will be
     * raised.
     *
     * @throws VerifyConfigProviderException
     *             in case this configuration is not valid.
     */
    public void doVerify() throws VerifyConfigProviderException {
        for (DockerRegistry registry : registries) {
            registry.doVerify();
        }
    }

    @Extension
    public static class DockerConfigProvider extends JsonConfigProvider {

        @Override
        public String getDisplayName() {
            return Messages.DockerConfig_displayName();
        }

        @Override
        public Config newConfig(@NonNull String configId) {
            return new DockerConfig(configId, "MyDockerConfig", "user config", loadTemplateContent(), null);
        }

        protected String loadTemplateContent() {
            try (InputStream is = this.getClass().getResourceAsStream("template.dockerconfig")) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) { // NOSONAR
                return null;
            }
        }

        @Override
        public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
            String fileContent = configFile.content;
            if (configFile instanceof DockerConfig) {
                DockerConfig config = (DockerConfig) configFile;

                List<DockerRegistry> registries = config.getRegistries();

                if (!registries.isEmpty()) {
                    listener.getLogger().println("Adding all server entries");

                    DockerConfigHelper helper = new DockerConfigHelper(config.getRegistries());
                    Map<String, StandardUsernamePasswordCredentials> source2Credentials = helper.resolveCredentials(build);
                    fileContent = helper.fillRegistries(fileContent, source2Credentials);
                }

                try {
                    if (StringUtils.isNotBlank(fileContent)) { // NOSONAR
                        config.doVerify();
                    }
                } catch (VerifyConfigProviderException e) {
                    throw new AbortException("Invalid user config: " + e.getMessage());
                }
            }
            return fileContent;
        }

    }
}
