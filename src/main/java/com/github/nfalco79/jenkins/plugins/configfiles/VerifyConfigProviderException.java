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

/**
 * Signals an error in the a user configuration file when
 * {@link PyPIConfig#doVerify()} is called.
 *
 * @author Nikolas Falco
 * @since 1.0
 */
@SuppressWarnings("serial")
public class VerifyConfigProviderException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message
     *            the failure message.
     */
    public VerifyConfigProviderException(String message) {
        super(message);
    }

}
