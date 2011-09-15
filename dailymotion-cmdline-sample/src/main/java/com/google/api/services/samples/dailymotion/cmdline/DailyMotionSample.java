/*
 * Copyright (c) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.api.services.samples.dailymotion.cmdline;

import com.google.api.client.auth.oauth2.draft10.AuthorizationRequestUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.Json;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;


/**
 * A sample application that demonstrates how the Google OAuth2 library can be used to
 * authenticate against Daily Motion.
 *
 * @author Ravi Mistry
 */
public class DailyMotionSample {

  /** OAuth 2 scope. */
  private static final String SCOPE = "read";

  private static void run(JsonFactory jsonFactory) throws Exception {
    // authorization
    HttpTransport transport = new NetHttpTransport();
    VerificationCodeReceiver receiver = new LocalServerReceiver();
    try {
      String redirectUrl = receiver.getRedirectUrl();
      launchInBrowser(
          "google-chrome",
          redirectUrl,
          OAuth2ClientCredentials.CLIENT_ID,
          SCOPE);
      exchangeCodeForAccessToken(redirectUrl,
          receiver,
          transport,
          jsonFactory,
          OAuth2ClientCredentials.CLIENT_ID,
          OAuth2ClientCredentials.CLIENT_SECRET);
      System.out.println("Authentication with DailyMotion was successful!");
    } finally {
      receiver.stop();
    }
  }

  private static void exchangeCodeForAccessToken(String redirectUrl,
    VerificationCodeReceiver receiver,
    HttpTransport transport,
    JsonFactory jsonFactory,
    String clientId,
    String clientSecret) throws IOException {
    String code = receiver.waitForCode();
    System.out.println("The access token is: " + code);
  }

  private static void launchInBrowser(
      String browser, String redirectUrl, String clientId, String scope) throws IOException {
    String authorizationUrl = new DailyMotionAuthorizationRequestUrl(
        clientId, redirectUrl, scope).build();
    if (Desktop.isDesktopSupported()) {
      Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Action.BROWSE)) {
        desktop.browse(URI.create(authorizationUrl));
        return;
      }
    }
    if (browser != null) {
      Runtime.getRuntime().exec(new String[] {browser, authorizationUrl});
    } else {
      System.out.println("Open the following address in your favorite browser:");
      System.out.println("  " + authorizationUrl);
    }
  }

  public static void main(String[] args) {
    JsonFactory jsonFactory = new JacksonFactory();
    try {
      try {
        OAuth2ClientCredentials.errorIfNotSpecified();
        // TODO(rmistry): Also demonstrate how to access a protected resource once you have an
        // access token.
        run(jsonFactory);
        // Success!
        return;
      } catch (HttpResponseException e) {
        System.err.println(e.getResponse().parseAsString());
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(0);
  }
}
