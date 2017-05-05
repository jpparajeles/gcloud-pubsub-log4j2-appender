package io.imaravic.log4j.pubsub;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.PublishRequest;
import com.google.api.services.pubsub.model.PubsubMessage;

import io.imaravic.log4j.util.GoogleCloudCredentials;
import io.imaravic.log4j.pubsub.GoogleCloudPubsubManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.List;

import io.imaravic.log4j.util.GoogleCloudMetadata;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudPubsubManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudPubsubManagerTest {
  @Mock
  HttpTransport httpTransport;

  @Mock
  GoogleCloudMetadata googleCloudMetadata;

  @Mock
  GoogleCloudCredentials googleCloudCredentials;

  @Mock
  Pubsub pubsubClient;

  @Before
  public void setup() throws Exception {
    PowerMockito.spy(GoogleCloudPubsubManager.class);
    PowerMockito.doReturn(pubsubClient).when(GoogleCloudPubsubManager.class,
                                             "createPubsubClient",
                                             any(HttpTransport.class),
                                             any(GoogleCloudCredentials.class),
                                             anyInt());
  
    

    when(googleCloudMetadata.fetchFromPath("project/project-id"))
        .thenReturn("project_id");

    when(googleCloudCredentials.usingComputeCredentials())
        .thenReturn(true);
    
    when(googleCloudCredentials.getServiceAccountId())
        .thenReturn(null);
  }

  @Test
  public void testBootstrappingManagerFromGCEOnCompute() throws Exception {
    when(googleCloudMetadata.fetchFromPath("instance/attributes/"))
        .thenReturn("");
    
    GoogleCloudPubsubManager googleCloudPubsubManager =
          PowerMockito.spy(new GoogleCloudPubsubManager("name",
                                                      httpTransport,
                                                      googleCloudMetadata,
                                                      googleCloudCredentials,
                                                      null,
                                                      "topic",
                                                      false,
                                                      1));
    
    doNothing().when(googleCloudPubsubManager)
        .writeToGoogleCloudLogging(any(PublishRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.000Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudPubsubManager.write(event);

    ArgumentCaptor<PublishRequest> publishRequestCaptor =
        ArgumentCaptor.forClass(PublishRequest.class);
    verify(googleCloudPubsubManager).writeToGoogleCloudLogging(publishRequestCaptor.capture());

    List<PubsubMessage> entries = publishRequestCaptor.getValue().getMessages();
    assertEquals(1, entries.size());

    assertEquals("projects/project_id/topics/topic",
                 googleCloudPubsubManager.getFullyDefinedTopicName());
    assertEquals("LogMsg", new String(entries.get(0).decodeData(), "UTF-8"));
  }

  @Test
  public void testBatchingFromManager() throws Exception {
    when(googleCloudMetadata.fetchFromPath("instance/attributes/"))
        .thenReturn("");

    GoogleCloudPubsubManager googleCloudPubsubManager =
        PowerMockito.spy(new GoogleCloudPubsubManager("name",
                                                      httpTransport,
                                                      googleCloudMetadata,
                                                      googleCloudCredentials,
                                                      null,
                                                      "topic",
                                                      false,
                                                      1));

    doNothing().when(googleCloudPubsubManager)
        .writeToGoogleCloudLogging(any(PublishRequest.class));

    final int batchSize = 128;
    for (int i = 0; i < batchSize - 1; ++i) {
      googleCloudPubsubManager.write(buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO));
      verify(googleCloudPubsubManager, never())
          .writeToGoogleCloudLogging(any(PublishRequest.class));
    }

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudPubsubManager.write(event);

    ArgumentCaptor<PublishRequest> publishRequestCaptor =
        ArgumentCaptor.forClass(PublishRequest.class);
    verify(googleCloudPubsubManager).writeToGoogleCloudLogging(publishRequestCaptor.capture());

    List<PubsubMessage> entries = publishRequestCaptor.getValue().getMessages();
    assertEquals(batchSize, entries.size());
  }

  @Test(expected = AppenderLoggingException.class)
  public void testExceptionIsThrownOnExceptionFromLoggingClient() throws Exception {
    when(googleCloudMetadata.fetchFromPath("instance/attributes/"))
        .thenReturn("");

    GoogleCloudPubsubManager googleCloudPubsubManager =
        PowerMockito.spy(new GoogleCloudPubsubManager("name",
                                                      httpTransport,
                                                      googleCloudMetadata,
                                                      googleCloudCredentials,
                                                      null,
                                                      "topic",
                                                      false,
                                                      1));

    doThrow(new IOException("TEST")).when(googleCloudPubsubManager)
        .writeToGoogleCloudLogging(any(PublishRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudPubsubManager.write(event);
  }

  @Test
  public void testBootstrappingManagerNotFromGCE() throws Exception {
    when(googleCloudCredentials.usingComputeCredentials())
        .thenReturn(false);
    when(googleCloudCredentials.getServiceAccountId())
        .thenReturn("user@gcloud.com");

    when(googleCloudMetadata.fetchFromPath(anyString()))
        .thenThrow(new IOException("TEST"));

    GoogleCloudPubsubManager googleCloudPubsubManager =
        PowerMockito.spy(new GoogleCloudPubsubManager("name",
                                                      httpTransport,
                                                      googleCloudMetadata,
                                                      googleCloudCredentials,
                                                      "_project_id_",
                                                      "topic",
                                                      false,
                                                      1));

    doNothing().when(googleCloudPubsubManager)
        .writeToGoogleCloudLogging(any(PublishRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.000Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudPubsubManager.write(event);

    ArgumentCaptor<PublishRequest> publishRequestCaptor =
        ArgumentCaptor.forClass(PublishRequest.class);
    verify(googleCloudPubsubManager).writeToGoogleCloudLogging(publishRequestCaptor.capture());

    List<PubsubMessage> entries = publishRequestCaptor.getValue().getMessages();
    assertEquals(1, entries.size());

    assertEquals("projects/_project_id_/topics/topic",
                 googleCloudPubsubManager.getFullyDefinedTopicName());
    assertEquals("LogMsg", new String(entries.get(0).decodeData(), "UTF-8"));
  }

  private static Log4jLogEvent buildLogEvent(final String logMsg,
                                             final String timestamp,
                                             final Level level) {
    return Log4jLogEvent.newBuilder().setLoggerName("loggerName")
        .setLoggerFqcn("loggerFQCN")
        .setLevel(level)
        .setMessage(new SimpleMessage(logMsg))
        .setTimeMillis(new DateTime(timestamp).getValue()).build();
  }
}