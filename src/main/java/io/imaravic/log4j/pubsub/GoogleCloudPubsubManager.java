/*
 * Copyright (c) 2015 Igor Maravić
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.imaravic.log4j.pubsub;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.PubsubScopes;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PubsubMessage;
import com.google.api.services.pubsub.model.Topic;
import com.google.common.annotations.VisibleForTesting;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.util.List;

import io.imaravic.log4j.pubsub.util.GoogleCloudMetadata;
import io.imaravic.log4j.pubsub.util.RetryHttpInitializerWrapper;

public class GoogleCloudPubsubManager extends AbstractManager {
  private static final String APPLICATION_NAME = "GoogleCloudPubsub-Log4j2Appender";

  private List<PubsubMessage> pubsubMessagesBuffer = Lists.newArrayList();
  private final String fullyDefinedTopicName;
  private final Pubsub pubsubClient;

  @VisibleForTesting
  GoogleCloudPubsubManager(final String name,
                           final HttpTransport transport,
                           final GoogleCloudMetadata googleCloudMetadata,
                           final GoogleCloudCredentials googleCloudCredentials,
                           final String googleCloudProjectId,
                           final String topic,
                           final boolean autoCreateTopic,
                           final int maxRetryTimeMillis)
      throws GeneralSecurityException, IOException {
    super(null, name);

    fullyDefinedTopicName =
        createFullyDefinedTopicName(
            getGoogleCloudProjectId(googleCloudProjectId, googleCloudMetadata),
            topic);

    this.pubsubClient = createPubsubClient(transport,
                                           googleCloudCredentials,
                                           maxRetryTimeMillis);
    if (autoCreateTopic) {
      createTopic();
    }
  }

  private static String createFullyDefinedTopicName(final String googleCloudProjectId,
                                             final String topic) {
    return "projects/" + googleCloudProjectId + "/topics/" + topic;
  }

  private void createTopic() throws IOException{
    try {
      pubsubClient.projects()
          .topics()
          .create(fullyDefinedTopicName, new Topic())
          .execute();
    } catch (final GoogleJsonResponseException e) {
      if (e.getDetails().getCode() != 409) {
        throw e;
      }
    }
  }

  public synchronized void write(final LogEvent event) {
    final String logMsg = event.getMessage().getFormattedMessage();

    final PubsubMessage pubsubMessage = new PubsubMessage();
    pubsubMessage.encodeData(logMsg.getBytes(Charset.forName("UTF-8")));

    pubsubMessagesBuffer.add(pubsubMessage);

    if (event.isEndOfBatch()) {
      final List<PubsubMessage> entriesToWrite = pubsubMessagesBuffer;
      pubsubMessagesBuffer = Lists.newArrayList();

      final PublishRequest publishRequest =
          new PublishRequest().setMessages(entriesToWrite);
      try {
        writeToGoogleCloudLogging(publishRequest);
      } catch (final IOException e) {
        throw new AppenderLoggingException("Publishing message to topic " +
                                           "\"" + fullyDefinedTopicName + "\" failed", e);
      }
    }
  }

  @VisibleForTesting
  void writeToGoogleCloudLogging(final PublishRequest publishRequest)
      throws IOException {
    pubsubClient.projects()
        .topics()
        .publish(fullyDefinedTopicName,
                 publishRequest)
        .execute();
  }

  public static GoogleCloudPubsubManager getManager(final String name,
                                                     final GoogleCloudCredentials googleCloudCredentials,
                                                     final String googleCloudProjectId,
                                                     final String topic,
                                                     final boolean autoCreateTopic,
                                                     final int maxRetryTimeMillis) {
    return AbstractManager.getManager(
        name,
        new ManagerFactory<GoogleCloudPubsubManager, Object>() {
          @Override
          public GoogleCloudPubsubManager createManager(String name,
                                                         Object data) {
            try {
              final HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
              final GoogleCloudMetadata googleCloudMetadata = new GoogleCloudMetadata(transport);
              return new GoogleCloudPubsubManager(name,
                                                   transport,
                                                   googleCloudMetadata,
                                                   googleCloudCredentials,
                                                   googleCloudProjectId,
                                                   topic,
                                                   autoCreateTopic,
                                                   maxRetryTimeMillis);
            } catch (final Throwable e) {
              LOGGER.error("Failed to initialize GoogleCloudLoggingManager", e);
            }
            return null;
          }
        },
        null);
  }

  private static String getGoogleCloudProjectId(final String googleCloudProjectId,
                                                final GoogleCloudMetadata googleCloudMetadata)
      throws IOException {
    if (googleCloudProjectId == null) {
      return googleCloudMetadata.fetchFromPath("project/project-id");
    } else {
      return googleCloudProjectId;
    }
  }

  private static Pubsub createPubsubClient(final HttpTransport transport,
                                            final GoogleCloudCredentials credentials,
                                            final int maxRetryTimeMillis)
      throws GeneralSecurityException, IOException {
    final JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
    return new Pubsub.Builder(transport,
                               jacksonFactory,
                               new RetryHttpInitializerWrapper(
                                   credentials.getCredential(transport,
                                                             jacksonFactory,
                                                             PubsubScopes.all()),
                                   maxRetryTimeMillis))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }

  @VisibleForTesting
  String getFullyDefinedTopicName() {
    return fullyDefinedTopicName;
  }
}
