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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.lib.configprovider.AbstractConfigProviderImpl;
import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.lib.configprovider.model.ContentType;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.github.nfalco79.jenkins.plugins.configfiles.util.CredentialsUtil;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.AbortException;
import hudson.Extension;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

/**
 * A config/provider to handle the special case of a gem config file.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class GemConfig extends Config {
    private static final long serialVersionUID = 1L;

    private String apiKey;
    private final List<GemSource> sources;

    @DataBoundConstructor
    public GemConfig(@NonNull String id, String name, String comment, String content, List<GemSource> sources) {
        super(id, Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(comment), Util.fixEmptyAndTrim(content));
        this.sources = sources == null ? new ArrayList<>(3) : sources;
    }


    public List<GemSource> getSources() {
        return sources;
    }

    public String getApiKey() {
        return apiKey;
    }

    @DataBoundSetter
    public void setApiKey(String apiKey) {
        this.apiKey = Util.fixEmptyAndTrim(apiKey);
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
        for (GemSource source : sources) {
            source.doVerify();
        }
    }

    @Extension
    public static class GemConfigProvider extends AbstractConfigProviderImpl {

        public GemConfigProvider() {
            load();
        }

        @Override
        public ContentType getContentType() {
            return null;
        }

        @Override
        public String getDisplayName() {
            return Messages.GemConfig_displayName();
        }

        @Override
        public Config newConfig(@NonNull String configId) {
            return new GemConfig(configId, "MyGemConfig", "user config", loadTemplateContent(), null);
        }

        public FormValidation doCheckApiKey(@CheckForNull @AncestorInPath Item item, @QueryParameter String apiKey) {
            if (item == null) {
                if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                    return FormValidation.ok();
                }
            } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
            // apiKey is not required, this method differs from than one in the CredentialsUtil
            if (StringUtils.isBlank(apiKey)) {
                return FormValidation.ok();
            }

            List<DomainRequirement> domainRequirement = Collections.emptyList();
            if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, //
                    item, item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM, //
                    domainRequirement, //
                    CredentialsMatchers.withId(apiKey)).isEmpty()) {
                return FormValidation.error(Messages.invalidCredentialsId());
            }
            return FormValidation.ok();
        }

        public ListBoxModel doFillApiKeyItems(final @AncestorInPath Item item, @QueryParameter String apiKey) {
            return CredentialsUtil.doFillCredentialsIdItems(item, apiKey, null);
        }

        protected String loadTemplateContent() {
            try (InputStream is = this.getClass().getResourceAsStream("template.gemrc")) {
                return IOUtils.toString(is, StandardCharsets.UTF_8);
            } catch (IOException e) { // NOSONAR
                return null;
            }
        }

        @Override
        public String supplyContent(Config configFile, Run<?, ?> build, FilePath workDir, TaskListener listener, List<String> tempFiles) throws IOException {
            String fileContent = configFile.content;
            if (configFile instanceof GemConfig) {
                GemConfig config = (GemConfig) configFile;

                GemConfigHelper helper = new GemConfigHelper(config.getSources());

                List<GemSource> sources = config.getSources();
                if (!sources.isEmpty()) {
                    listener.getLogger().println("Adding all server entries");
                    Map<String, StandardUsernamePasswordCredentials> source2Credentials = helper.resolveCredentials(build);
                    fileContent = helper.fillSources(fileContent, source2Credentials);
                }

                String apiKey = config.getApiKey();
                if (apiKey != null) {
                    listener.getLogger().println("Adding API Key entry");
                    fileContent = helper.fillApiKey(fileContent, apiKey, build);
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
