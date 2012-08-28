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
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.FileCredentialStore;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.File;
import java.io.IOException;


/**
 * A sample application that demonstrates how the Google OAuth2 library can be used to authenticate
 * against Daily Motion.
 * 
 * @author Ravi Mistry
 */
public class DailyMotionSample {

  /** OAuth 2 scope. */
  private static final String SCOPE = "read";

  /** Local server host name to use. */
  private static final String HOST = "127.0.0.1";

  /** Local server port to use. */
  private static final int PORT = 8080;

  /** Global instance of the HTTP transport. */
  private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

  /** Global instance of the JSON factory. */
  static final JsonFactory JSON_FACTORY = new JacksonFactory();

  private static final String TOKEN_SERVER_URL = "https://api.dailymotion.com/oauth/token";

  private static final String AUTHORIZATION_SERVER_URL =
      "https://api.dailymotion.com/oauth/authorize";

  private static void run() throws Exception {
    // authorization
    final Credential credential = authorize();

    HttpRequestFactory requestFactory =
        HTTP_TRANSPORT.createRequestFactory(new HttpRequestInitializer() {
            @Override
          public void initialize(HttpRequest request) throws IOException {
            credential.initialize(request);
            request.setParser(new JsonObjectParser(JSON_FACTORY));
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
  }

  private static Credential authorize() throws Exception {
    AuthorizationCodeFlow codeFlow = new AuthorizationCodeFlow.Builder(BearerToken
        .authorizationHeaderAccessMethod(),
        HTTP_TRANSPORT,
        JSON_FACTORY,
        new GenericUrl(TOKEN_SERVER_URL),
        new ClientParametersAuthentication(
            ClientCredentials.CLIENT_ID, ClientCredentials.CLIENT_SECRET),
        ClientCredentials.CLIENT_ID,
        AUTHORIZATION_SERVER_URL).setScopes(SCOPE).setCredentialStore(new FileCredentialStore(
        new File(System.getProperty("user.home"), ".credentials/dailymotion.json"), JSON_FACTORY))
        .build();
    LocalServerReceiver receiver =
        new LocalServerReceiver.Builder().setHost(HOST).setPort(PORT).build();
    return new AuthorizationCodeInstalledApp(codeFlow, receiver).authorize("user");
  }

  public static void main(String[] args) {
    try {
      try {
        ClientCredentials.errorIfNotSpecified();
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
