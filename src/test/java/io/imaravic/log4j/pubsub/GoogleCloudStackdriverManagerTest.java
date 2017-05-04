package io.imaravic.log4j.pubsub;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.DateTime;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.api.services.logging.v2.model.WriteLogEntriesRequest;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.*;


/**
 * Created by aleph on 5/4/17.
 * Singularities
 * Spark Discoverer
 * Version 0.2 Coyote
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudStackdriverManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudStackdriverManagerTest {
  @Mock
  HttpTransport httpTransport;

  @Mock
  GoogleCloudCredentials googleCloudCredentials;

  @Mock
  Logging stackdriverLoggingClient;

  @Before
  public void setup() throws Exception {
    PowerMockito.spy(GoogleCloudStackdriverManager.class);
    
    PowerMockito.doReturn(stackdriverLoggingClient).when(GoogleCloudStackdriverManager.class,
        "createStackdriverLoggingClient",
        any(HttpTransport.class),
        any(GoogleCloudCredentials.class),
//        anyString(),anyString(),
        anyInt());
    PowerMockito.doReturn("THIS WAS INTERCEPTED yay").when(GoogleCloudStackdriverManager.class,
      "testInterception");
    
  
    when(googleCloudCredentials.usingComputeCredentials())
        .thenReturn(true);
    when(googleCloudCredentials.getServiceAccountId())
        .thenReturn(null);
  }

  @Test
  public void testBootstrappingManagerFromGCEOnCompute() throws Exception {
    System.out.println(googleCloudCredentials.toString());
    System.out.println("CALLING CONSTURCTOR, SHOULD BE INTERCEPTED");
    GoogleCloudStackdriverManager googleCloudStackdriverManager =
        PowerMockito.spy(new GoogleCloudStackdriverManager("name",
            httpTransport,
            googleCloudCredentials,
            "gce",
            "projects/[PROJECT_ID]/logs/[LOG_ID]",
            1));

    doNothing().when(googleCloudStackdriverManager)
        .writeToGoogleCloudLogging(any(WriteLogEntriesRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.000Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudStackdriverManager.write(event, PatternLayout.createDefaultLayout());

    ArgumentCaptor<WriteLogEntriesRequest> writeLogEntriesRequestArgumentCaptor =
        ArgumentCaptor.forClass(WriteLogEntriesRequest.class);
    verify(googleCloudStackdriverManager).writeToGoogleCloudLogging(writeLogEntriesRequestArgumentCaptor.capture());

    List<LogEntry> entries = writeLogEntriesRequestArgumentCaptor.getValue().getEntries();
    assertEquals(1, entries.size());

    assertEquals("LogMsg", entries.get(0).getTextPayload(), "UTF-8");
  }

  @Test
  public void testBatchingFromManager() throws Exception {

    GoogleCloudStackdriverManager googleCloudStackdriverManager =
        PowerMockito.spy(new GoogleCloudStackdriverManager("name",
            httpTransport,
            googleCloudCredentials,
            "gce",
            "projects/[PROJECT_ID]/logs/[LOG_ID]",
            1));


    doNothing().when(googleCloudStackdriverManager)
        .writeToGoogleCloudLogging(any(WriteLogEntriesRequest.class));

    final int batchSize = 128;
    for (int i = 0; i < batchSize - 1; ++i) {
      googleCloudStackdriverManager.write(
          buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO),
          PatternLayout.createDefaultLayout());
      verify(googleCloudStackdriverManager, never())
          .writeToGoogleCloudLogging(any(WriteLogEntriesRequest.class));
    }

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudStackdriverManager.write(event, PatternLayout.createDefaultLayout());

    ArgumentCaptor<WriteLogEntriesRequest> writeLogEntriesRequestArgumentCaptor =
        ArgumentCaptor.forClass(WriteLogEntriesRequest.class);
    verify(googleCloudStackdriverManager).writeToGoogleCloudLogging(writeLogEntriesRequestArgumentCaptor.capture());

    List<LogEntry> entries = writeLogEntriesRequestArgumentCaptor.getValue().getEntries();
    assertEquals(batchSize, entries.size());
  }

  @Test(expected = AppenderLoggingException.class)
  public void testExceptionIsThrownOnExceptionFromLoggingClient() throws Exception {
    GoogleCloudStackdriverManager googleCloudStackdriverManager =
        PowerMockito.spy(new GoogleCloudStackdriverManager("name",
            httpTransport,
            googleCloudCredentials,
            "gce",
            "projects/[PROJECT_ID]/logs/[LOG_ID]",
            1));
  
  
    doThrow(new IOException("TEST")).when(googleCloudStackdriverManager)
        .writeToGoogleCloudLogging(any(WriteLogEntriesRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.002Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudStackdriverManager.write(event, PatternLayout.createDefaultLayout());
  }

  @Test
  public void testBootstrappingManagerNotFromGCE() throws Exception {
    when(googleCloudCredentials.usingComputeCredentials())
        .thenReturn(false);
    when(googleCloudCredentials.getServiceAccountId())
        .thenReturn("user@gcloud.com");
  
    GoogleCloudStackdriverManager googleCloudStackdriverManager =
        PowerMockito.spy(new GoogleCloudStackdriverManager("name",
            httpTransport,
            googleCloudCredentials,
            "gce",
            "projects/[PROJECT_ID]/logs/[LOG_ID]",
            1));
  
  
    doNothing().when(googleCloudStackdriverManager)
        .writeToGoogleCloudLogging(any(WriteLogEntriesRequest.class));

    LogEvent event = buildLogEvent("LogMsg", "2015-04-06T18:38:24.000Z", Level.INFO);
    event.setEndOfBatch(true);

    googleCloudStackdriverManager.write(event, PatternLayout.createDefaultLayout());

    ArgumentCaptor<WriteLogEntriesRequest> writeLogEntriesRequestArgumentCaptor =
        ArgumentCaptor.forClass(WriteLogEntriesRequest.class);
    verify(googleCloudStackdriverManager).writeToGoogleCloudLogging(writeLogEntriesRequestArgumentCaptor.capture());

    List<LogEntry> entries = writeLogEntriesRequestArgumentCaptor.getValue().getEntries();
    assertEquals(1, entries.size());

    assertEquals("LogMsg", entries.get(0).getTextPayload());
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