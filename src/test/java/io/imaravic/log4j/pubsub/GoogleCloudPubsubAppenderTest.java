package io.imaravic.log4j.pubsub;

import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudPubsubManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudPubsubAppenderTest {

  @Test
  public void testGoogleCloudLoggingAppenderBuilderWithDefaultsGetsBootstrappedSuccessfully() throws Exception {
    mockStatic(GoogleCloudPubsubManager.class);
    final GoogleCloudPubsubManager googleCloudPubsubManager = mock(GoogleCloudPubsubManager.class);
    when(GoogleCloudPubsubManager.getManager(anyString(),
                                             any(GoogleCloudCredentials.class),
                                             anyString(),
                                             anyString(),
                                             anyBoolean(),
                                             anyInt())).thenReturn(googleCloudPubsubManager);

    final GoogleCloudPubsubAppender appender = GoogleCloudPubsubAppender.newBuilder().build();

    final LogEvent logEvent = mock(LogEvent.class);
    appender.append(logEvent);

    verify(googleCloudPubsubManager).write(eq(logEvent));
  }

  @Test
  public void testGoogleCloudLoggingAppenderBuilderInCaseOfAnExceptionReturnsNull() throws Exception {
    mockStatic(GoogleCloudPubsubManager.class);
    when(GoogleCloudPubsubManager.getManager(anyString(),
                                             any(GoogleCloudCredentials.class),
                                             anyString(),
                                             anyString(),
                                             anyBoolean(),
                                             anyInt())).thenThrow(new RuntimeException("TEST"));

    final GoogleCloudPubsubAppender appender = GoogleCloudPubsubAppender.newBuilder().build();
    assertEquals(null, appender);
  }
}