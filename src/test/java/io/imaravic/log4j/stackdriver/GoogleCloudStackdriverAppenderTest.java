package io.imaravic.log4j.stackdriver;

import io.imaravic.log4j.util.GoogleCloudCredentials;
import org.apache.logging.log4j.core.LogEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Created by aleph on 5/4/17.
 * Singularities
 * Spark Discoverer
 * Version 0.2 Coyote
 */


@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudStackdriverManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudStackdriverAppenderTest {
  @Test
  public void testGoogleCloudLoggingAppenderBuilderWithDefaultsGetsBootstrappedSuccessfully() throws Exception {
    mockStatic(GoogleCloudStackdriverManager.class);
    final GoogleCloudStackdriverManager googleCloudStackdriverManager = mock(GoogleCloudStackdriverManager.class);
    when(GoogleCloudStackdriverManager.getManager(anyString(),
        any(GoogleCloudCredentials.class),
        anyString(),
        anyString(),
        anyInt())).thenReturn(googleCloudStackdriverManager);
    
    final GoogleCloudStackdriverAppender appender = GoogleCloudStackdriverAppender.newBuilder().build();
    
    final LogEvent logEvent = mock(LogEvent.class);
    appender.append(logEvent);
    
    verify(googleCloudStackdriverManager).write(eq(logEvent),eq(appender.getLayout()));
  }
  
  @Test
  public void testGoogleCloudLoggingAppenderBuilderInCaseOfAnExceptionReturnsNull() throws Exception {
    mockStatic(GoogleCloudStackdriverManager.class);
    when(GoogleCloudStackdriverManager.getManager(anyString(),
        any(GoogleCloudCredentials.class),
        anyString(),
        anyString(),
        anyInt())).thenThrow(new RuntimeException("TEST"));
    
    final GoogleCloudStackdriverAppender appender = GoogleCloudStackdriverAppender.newBuilder().build();
    assertEquals(null, appender);
  }
  
}