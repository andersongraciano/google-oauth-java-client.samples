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

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;


/**
 * A sample application that demonstrates how the Google OAuth2 library can be used to authenticate
 * against Daily Motion.
 *
 * @author Ravi Mistry
 */
public class DailyMotionSample {

  /** OAuth 2 scope. */
  private static final String SCOPE = "read";

  /** Global instance of the HTTP transport. */
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  /** Global instance of the JSON factory. */
  static final JsonFactory JSON_FACTORY = new JacksonFactory();

  private static final String TOKEN_SERVER_URL = "https://api.dailymotion.com/oauth/token";
  private static final String AUTHORIZATION_SERVER_URL =
      "https://api.dailymotion.com/oauth/authorize";

  private static void run() throws Exception {
    // authorization
    VerificationCodeReceiver receiver = new LocalServerReceiver();
    try {
      String redirectUri = receiver.getRedirectUri();
      launchInBrowser("google-chrome", redirectUri, OAuth2ClientCredentials.CLIENT_ID, SCOPE);

      final Credential credential = authorize(receiver, redirectUri);

      HttpRequestFactory requestFactory =
          HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
              credential.initialize(request);
              request.addParser(new JsonHttpParser(JSON_FACTORY));
            }
          });

      DailyMotionUrl url = new DailyMotionUrl("https://api.dailymotion.com/videos/favorites");
      url.setFields("id,tags,title,url");

      HttpRequest request = requestFactory.buildGetRequest(url);
      VideoFeed videoFeed = request.execute().parseAs(VideoFeed.class);
      if (videoFeed.list.isEmpty()) {
        System.out.println("No favorite videos found.");
      } else {
        if (videoFeed.hasMore) {
          System.out.print("First ");
        }
        System.out.println(videoFeed.list.size() + " favorite videos found:");
        for (Video video : videoFeed.list) {
          System.out.println();
          System.out.println("-----------------------------------------------");
          System.out.println("ID: " + video.id);
          System.out.println("Title: " + video.title);
          System.out.println("Tags: " + video.tags);
          System.out.println("URL: " + video.url);
        }
      }
    } finally {
      receiver.stop();
    }
  }

  private static Credential authorize(VerificationCodeReceiver receiver, String redirectUri)
      throws IOException {
    String code = receiver.waitForCode();
    AuthorizationCodeFlow codeFlow =
        new AuthorizationCodeFlow.Builder(BearerToken.authorizationHeaderAccessMethod(),
            HTTP_TRANSPORT,
            JSON_FACTORY,
            new GenericUrl(TOKEN_SERVER_URL),
            new ClientParametersAuthentication(
                OAuth2ClientCredentials.CLIENT_ID, OAuth2ClientCredentials.CLIENT_SECRET),
            OAuth2ClientCredentials.CLIENT_ID,
            AUTHORIZATION_SERVER_URL).setScopes(Arrays.asList(SCOPE)).build();

    TokenResponse response = codeFlow.newTokenRequest(code)
        .setRedirectUri(redirectUri).setScopes(Arrays.asList(SCOPE)).execute();

    return codeFlow.createAndStoreCredential(response, null);
  }

  private static void launchInBrowser(
      String browser, String redirectUrl, String clientId, String scope) throws IOException {
    String authorizationUrl = new AuthorizationCodeRequestUrl(
        AUTHORIZATION_SERVER_URL, clientId).setRedirectUri(redirectUrl)
        .setScopes(Arrays.asList(scope)).build();
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
    try {
      try {
        OAuth2ClientCredentials.errorIfNotSpecified();
        run();
        // Success!
        return;
      } catch (IOException e) {
        System.err.println(e.getMessage());
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(1);
  }
}
