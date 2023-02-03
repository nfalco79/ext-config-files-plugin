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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.DataBoundConstructor;

import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Run;
import hudson.model.TaskListener;

/**
 * A config/provider to handle the special case of a PyPIrc file
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class PyPIConfig extends Config {
    private static final long serialVersionUID = 1L;

    private final List<PyPIServer> servers;

    @DataBoundConstructor
    public PyPIConfig(@NonNull String id, String name, String comment, String content, List<PyPIServer> servers) {
        super(id, Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(comment), Util.fixEmptyAndTrim(content));
        this.servers = servers == null ? new ArrayList<>(3) : servers;
    }

    public List<PyPIServer> getServers() {
        return servers;
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
        for (PyPIServer registry : servers) {
            registry.doVerify();
        }
    }

    @Extension
    public static class PyPIConfigProvider extends AbstractConfigProviderImpl {

        public PyPIConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.PyPircConfig_displayName();
        }

        @Override
        public Config newConfig(@NonNull String configId) {
            return new PyPIConfig(configId, "MyPypircConfig", "user config", loadTemplateContent(), null);
        }

        protected String loadTemplateContent() {
            try (InputStream is = this.getClass().getResourceAsStream("template.pypirc")) {
                return IOUtils.toString(is, "UTF-8");
            } catch (IOException e) { // NOSONAR
                return null;
            }
        }

        @Override
        public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
            String fileContent = configFile.content;
            if (configFile instanceof PyPIConfig) {
                PyPIConfig config = (PyPIConfig) configFile;

                List<PyPIServer> servers = config.getServers();
                if (!servers.isEmpty()) {
                    PyPIServerHelper helper = new PyPIServerHelper(servers);
                    listener.getLogger().println("Adding all server entries");
                    Map<String, StandardUsernameCredentials> registry2Credentials = helper.resolveCredentials(build);
                    fileContent = helper.fillRegistry(fileContent, registry2Credentials);
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
