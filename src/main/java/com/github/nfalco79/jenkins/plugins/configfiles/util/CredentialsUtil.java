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
package com.github.nfalco79.jenkins.plugins.configfiles.util;

import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;

import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import com.github.nfalco79.jenkins.plugins.configfiles.Messages;

import hudson.model.Item;
import hudson.model.Queue;
import hudson.model.queue.Tasks;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

public final class CredentialsUtil {

    private CredentialsUtil() {
    }

    public static FormValidation doCheckCredentialsId(@CheckForNull @AncestorInPath Item item,
                                               @QueryParameter String credentialsId,
                                               @QueryParameter String url) {
        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
            return FormValidation.ok();
        }
        if (StringUtils.isBlank(credentialsId)) {
            return FormValidation.warning(Messages.emptyCredentialsId());
        }

        List<DomainRequirement> domainRequirement = url != null ? URIRequirementBuilder.fromUri(url).build() : Collections.emptyList();
        if (CredentialsProvider.listCredentials(StandardUsernameCredentials.class, //
                item, getAuthentication(item), //
                domainRequirement, //
                CredentialsMatchers.withId(credentialsId)).isEmpty()) {
            return FormValidation.error(Messages.invalidCredentialsId());
        }
        return FormValidation.ok();
    }

    public static ListBoxModel doFillCredentialsIdItems(final @AncestorInPath Item item,
                                                 @QueryParameter String credentialsId,
                                                 final @QueryParameter String url) {
        StandardListBoxModel result = new StandardListBoxModel();

        credentialsId = StringUtils.trimToEmpty(credentialsId);
        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return result.includeCurrentValue(credentialsId);
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return result.includeCurrentValue(credentialsId);
            }
        }

        Authentication authentication = getAuthentication(item);
        List<DomainRequirement> domainRequirements = url != null ? URIRequirementBuilder.fromUri(url).build() : Collections.emptyList();
        CredentialsMatcher always = CredentialsMatchers.always();
        Class<StandardUsernameCredentials> type = StandardUsernameCredentials.class;

        result.includeEmptyValue();
        if (item != null) {
            result.includeMatchingAs(authentication, item, type, domainRequirements, always);
        } else {
            result.includeMatchingAs(authentication, Jenkins.get(), type, domainRequirements, always);
        }
        return result;
    }

    private static Authentication getAuthentication(final Item item) {
        return item instanceof Queue.Task ? Tasks.getAuthenticationOf((Queue.Task) item) : ACL.SYSTEM;
    }

}
