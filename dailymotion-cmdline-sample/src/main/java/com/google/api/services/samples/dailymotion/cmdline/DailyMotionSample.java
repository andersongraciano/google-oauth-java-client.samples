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

import com.google.api.client.auth.oauth2.draft10.AccessProtectedResource;
import com.google.api.client.auth.oauth2.draft10.AccessProtectedResource.Method;
import com.google.api.client.auth.oauth2.draft10.AccessTokenErrorResponse;
import com.google.api.client.auth.oauth2.draft10.AccessTokenRequest.AuthorizationCodeGrant;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpParser;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.IOException;
import java.net.URI;


/**
 * A sample application that demonstrates how the Google OAuth2 library can be used to authenticate
 * against Daily Motion.
 *
 * @author Ravi Mistry
 */
public class DailyMotionSample {

  /** OAuth 2 scope. */
  private static final String SCOPE = "read";

  private static final String AUTHORIZATION_SERVER = "https://api.dailymotion.com/oauth/token";

  static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
  static final JsonFactory JSON_FACTORY = new JacksonFactory();

  private static void run(JsonFactory jsonFactory) throws Exception {
    // authorization
    VerificationCodeReceiver receiver = new LocalServerReceiver();
    try {
      String redirectUrl = receiver.getRedirectUrl();
      launchInBrowser("google-chrome", redirectUrl, OAuth2ClientCredentials.CLIENT_ID, SCOPE);
      AccessTokenResponse response = exchangeCodeForAccessToken(redirectUrl,
          receiver,
          HTTP_TRANSPORT,
          jsonFactory,
          OAuth2ClientCredentials.CLIENT_ID,
          OAuth2ClientCredentials.CLIENT_SECRET);
      final AccessProtectedResource initializer = new AccessProtectedResource(response.accessToken,
          Method.AUTHORIZATION_HEADER,
          HTTP_TRANSPORT,
          jsonFactory,
          AUTHORIZATION_SERVER,
          OAuth2ClientCredentials.CLIENT_ID,
          OAuth2ClientCredentials.CLIENT_SECRET,
          response.refreshToken);
      HttpRequestFactory requestFactory =
          HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {

            @Override
            public void initialize(HttpRequest request) throws IOException {
              initializer.initialize(request);
              request.addParser(new JsonHttpParser(JSON_FACTORY));
            }
          });
      DailyMotionUrl url = new DailyMotionUrl("https://api.dailymotion.com/videos/favorites");
      url.fields = "id,tags,title,url";
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

  private static AccessTokenResponse exchangeCodeForAccessToken(String redirectUrl,
      VerificationCodeReceiver receiver,
      HttpTransport transport,
      JsonFactory jsonFactory,
      String clientId,
      String clientSecret) throws IOException {
    String code = receiver.waitForCode();
    try {
      // exchange code for an access token
      return new AuthorizationCodeGrant(new NetHttpTransport(),
          jsonFactory,
          AUTHORIZATION_SERVER,
          clientId,
          clientSecret,
          code,
          redirectUrl).execute();
    } catch (HttpResponseException e) {
      AccessTokenErrorResponse response = e.getResponse().parseAs(AccessTokenErrorResponse.class);
      System.out.println();
      System.err.println("Error: " + response.error);
      System.out.println();
      System.exit(1);
      return null;
    }
  }

  private static void launchInBrowser(
      String browser, String redirectUrl, String clientId, String scope) throws IOException {
    String authorizationUrl =
        new DailyMotionAuthorizationRequestUrl(clientId, redirectUrl, scope).build();
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
        run(jsonFactory);
        // Success!
        return;
      } catch (HttpResponseException e) {
        System.err.println(e.getResponse().parseAsString());
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
    System.exit(1);
  }
}
