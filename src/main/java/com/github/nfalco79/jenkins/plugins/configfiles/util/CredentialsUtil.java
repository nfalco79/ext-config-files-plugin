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
package com.github.nfalco79.jenkins.plugins.configfiles.util;

import java.util.List;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.github.nfalco79.jenkins.plugins.configfiles.Messages;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.security.AccessControlled;
import hudson.security.Permission;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public final class CredentialsUtil {

    private CredentialsUtil() {
    }

    public static FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item projectOrFolder,
                                               @QueryParameter String credentialsId,
                                               @QueryParameter String url) {
        if ((projectOrFolder == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) ||
                (projectOrFolder != null && !projectOrFolder.hasPermission(Item.EXTENDED_READ) && !projectOrFolder.hasPermission(CredentialsProvider.USE_ITEM))) {
            return FormValidation.ok();
        }
        if (StringUtils.isBlank(credentialsId)) {
            return FormValidation.warning(Messages.emptyCredentialsId());
        }

        List<DomainRequirement> domainRequirement = URIRequirementBuilder.fromUri(url).build();
        if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, //
                projectOrFolder, //
                getAuthentication(projectOrFolder), //
                domainRequirement, //
                CredentialsMatchers.withId(credentialsId)).isEmpty()) {
            return FormValidation.error(Messages.invalidCredentialsId());
        }
        return FormValidation.ok();
    }

    public static ListBoxModel doFillCredentialsIdItems(final ItemGroup<?> context, //
                                                        final @AncestorInPath Item projectOrFolder, //
                                                        @QueryParameter String credentialsId, //
                                                        final @QueryParameter String url) {
        Permission permToCheck = projectOrFolder == null ? Jenkins.ADMINISTER : Item.CONFIGURE;
        AccessControlled contextToCheck = projectOrFolder == null ? Jenkins.get() : projectOrFolder;
        credentialsId = StringUtils.trimToEmpty(credentialsId);

        // If we're on the global page and we don't have administer
        // permission or if we're in a project or folder
        // and we don't have configure permission there
        if (!contextToCheck.hasPermission(permToCheck)) {
            return new StandardUsernameListBoxModel().includeCurrentValue(credentialsId);
        }

        Authentication authentication = getAuthentication(projectOrFolder);
        List<DomainRequirement> domainRequirements = URIRequirementBuilder.fromUri(url).build();
        Class<StandardUsernameCredentials> type = StandardUsernameCredentials.class;

        return new StandardListBoxModel() //
                .includeAs(authentication, projectOrFolder, type, domainRequirements) //
                .includeEmptyValue();
    }

    private static Authentication getAuthentication(final Item item) {
        return item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM;
    }

}
