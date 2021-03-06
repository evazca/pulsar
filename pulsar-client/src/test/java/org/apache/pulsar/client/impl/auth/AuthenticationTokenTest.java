/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
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
package org.apache.pulsar.client.impl.auth;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.google.common.base.Charsets;

import java.io.File;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.apache.pulsar.client.api.AuthenticationDataProvider;
import org.testng.annotations.Test;

public class AuthenticationTokenTest {

    @Test
    public void testAuthToken() throws Exception {
        AuthenticationToken authToken = new AuthenticationToken("token-xyz");
        assertEquals(authToken.getAuthMethodName(), "token");

        AuthenticationDataProvider authData = authToken.getAuthData();
        assertTrue(authData.hasDataFromCommand());
        assertEquals(authData.getCommandData(), "token-xyz");

        assertFalse(authData.hasDataForTls());
        assertNull(authData.getTlsCertificates());
        assertNull(authData.getTlsPrivateKey());

        assertTrue(authData.hasDataForHttp());
        assertEquals(authData.getHttpHeaders(),
                Collections.singletonMap("Authorization", "Bearer token-xyz").entrySet());

        authToken.close();
    }

    @Test
    public void testAuthTokenConfig() throws Exception {
        AuthenticationToken authToken = new AuthenticationToken();
        authToken.configure("token:my-test-token-string");
        assertEquals(authToken.getAuthMethodName(), "token");

        AuthenticationDataProvider authData = authToken.getAuthData();
        assertTrue(authData.hasDataFromCommand());
        assertEquals(authData.getCommandData(), "my-test-token-string");
        authToken.close();
    }

    @Test
    public void testAuthTokenConfigFromFile() throws Exception {
        File tokenFile = File.createTempFile("pular-test-token", ".key");
        tokenFile.deleteOnExit();
        FileUtils.write(tokenFile, "my-test-token-string", Charsets.UTF_8);

        AuthenticationToken authToken = new AuthenticationToken();
        authToken.configure("file://" + tokenFile);
        assertEquals(authToken.getAuthMethodName(), "token");

        AuthenticationDataProvider authData = authToken.getAuthData();
        assertTrue(authData.hasDataFromCommand());
        assertEquals(authData.getCommandData(), "my-test-token-string");

        // Ensure if the file content changes, the token will get refreshed as well
        FileUtils.write(tokenFile, "other-token", Charsets.UTF_8);

        AuthenticationDataProvider authData2 = authToken.getAuthData();
        assertTrue(authData2.hasDataFromCommand());
        assertEquals(authData2.getCommandData(), "other-token");

        authToken.close();
    }

    /**
     * File can have spaces and newlines before or after the token. We should be able to read
     * the token correctly anyway.
     */
    @Test
    public void testAuthTokenConfigFromFileWithNewline() throws Exception {
        File tokenFile = File.createTempFile("pular-test-token", ".key");
        tokenFile.deleteOnExit();
        FileUtils.write(tokenFile, "  my-test-token-string  \r\n", Charsets.UTF_8);

        AuthenticationToken authToken = new AuthenticationToken();
        authToken.configure("file://" + tokenFile);
        assertEquals(authToken.getAuthMethodName(), "token");

        AuthenticationDataProvider authData = authToken.getAuthData();
        assertTrue(authData.hasDataFromCommand());
        assertEquals(authData.getCommandData(), "my-test-token-string");

        // Ensure if the file content changes, the token will get refreshed as well
        FileUtils.write(tokenFile, "other-token", Charsets.UTF_8);

        AuthenticationDataProvider authData2 = authToken.getAuthData();
        assertTrue(authData2.hasDataFromCommand());
        assertEquals(authData2.getCommandData(), "other-token");

        authToken.close();
    }

    @Test
    public void testAuthTokenConfigNoPrefix() throws Exception {
        AuthenticationToken authToken = new AuthenticationToken();
        authToken.configure("my-test-token-string");
        assertEquals(authToken.getAuthMethodName(), "token");

        AuthenticationDataProvider authData = authToken.getAuthData();
        assertTrue(authData.hasDataFromCommand());
        assertEquals(authData.getCommandData(), "my-test-token-string");
        authToken.close();
    }
}
