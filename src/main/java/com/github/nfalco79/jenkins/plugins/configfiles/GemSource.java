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

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import com.github.nfalco79.jenkins.plugins.configfiles.util.CredentialsUtil;
import com.github.nfalco79.jenkins.plugins.configfiles.Messages;

import hudson.Extension;
import hudson.Util;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.util.FormValidation;
import hudson.util.FormValidation.Kind;
import hudson.util.ListBoxModel;

/**
 * Holder of all informations about a Gem source.
 * <p>
 * This class keep all necessary information to access a Gem source that must be
 * stored in a user config file. Typically information are:
 * <ul>
 * <li>the registry URL</li>
 * <li>account credentials to access the server</li>
 * </ul>
 *
 * @author Nikolas Falco
 * @since 1.0
 */
public class GemSource extends AbstractDescribableImpl<GemSource> implements Serializable {
    private static final long serialVersionUID = -5199710867477461372L;

    private final String url;
    private final String credentialsId;

    /**
     * Default constructor.
     *
     * @param url of a Gem source
     * @param credentialsId credentials identifier
     */
    @DataBoundConstructor
    public GemSource(@Nonnull String url, String credentialsId) {
        this.url = Util.fixEmpty(url);
        this.credentialsId = Util.fixEmpty(credentialsId);
    }

    /**
     * Get the server URL
     *
     * @return the server URL
     */
    @Nullable
    public String getUrl() {
        return url;
    }

    /**
     * Get credentials for this server.
     *
     * @return a credential identifier.
     */
    @Nullable
    public String getCredentialsId() {
        return credentialsId;
    }

    /**
     * Perform the validation of current registry.
     * <p>
     * If validation pass then no {@link VerifyConfigProviderException} will be
     * raised.
     *
     * @throws VerifyConfigProviderException in case this configuration is not
     *         valid.
     */
    public void doVerify() throws VerifyConfigProviderException {
        // recycle validations from descriptor
        DescriptorImpl descriptor = new DescriptorImpl();

        throwException(descriptor.doCheckUrl(url));
    }

    private void throwException(FormValidation form) throws VerifyConfigProviderException {
        if (form.kind == Kind.ERROR) {
            throw new VerifyConfigProviderException(form.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        return "url: " + url + (credentialsId != null ? " credentialId: " + credentialsId : "");
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<GemSource> {

        private Pattern variableRegExp = Pattern.compile("\\$\\{.*\\}");

        public FormValidation doCheckName(@CheckForNull @QueryParameter final String name) {
            if (StringUtils.isBlank(name)) {
                return FormValidation.error(Messages.PyPIServer_DescriptorImpl_emptyServerName());
            }
            if (name.matches("\\s")) {
                return FormValidation.error(Messages.PyPIServer_DescriptorImpl_invalidServerName());
            }
            return FormValidation.ok();
        }

        public FormValidation doCheckUrl(@CheckForNull @QueryParameter final String url) {
            if (StringUtils.isBlank(url)) {
                return FormValidation.error(Messages.emptyServerURL());
            }

            // test malformed URL
            if (!variableRegExp.matcher(url).find() && toURL(url) == null) {
                return FormValidation.error(Messages.invalidServerURL());
            }

            return FormValidation.ok();
        }

        public FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item item,
                                                   @QueryParameter String credentialsId,
                                                   @QueryParameter String serverUrl) {
            return CredentialsUtil.doCheckCredentialsId(item, credentialsId, serverUrl);
        }

        public ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item item,
                                                     @QueryParameter String credentialsId,
                                                     final @QueryParameter String url) {
            return CredentialsUtil.doFillCredentialsIdItems(item, credentialsId, url);
        }

        @Override
        public String getDisplayName() {
            return "";
        }

        private static URL toURL(final String url) {
            URL result = null;

            String fixedURL = Util.fixEmptyAndTrim(url);
            if (fixedURL != null) {
                try {
                    return new URL(fixedURL);
                } catch (MalformedURLException e) {
                    // no filter based on hostname
                }
            }

            return result;
        }

    }

}