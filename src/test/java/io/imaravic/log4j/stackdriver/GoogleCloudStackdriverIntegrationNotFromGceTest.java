package io.imaravic.log4j.stackdriver;

import io.imaravic.log4j.pubsub.GoogleCloudPubsubManager;
import io.imaravic.log4j.util.GoogleCloudCredentials;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudStackdriverManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudStackdriverIntegrationNotFromGceTest {
  @Before
  public void setup() throws Exception {
    mockStatic(GoogleCloudStackdriverManager.class);
    final GoogleCloudStackdriverManager googleCloudStackdriverManager = mock(GoogleCloudStackdriverManager.class);
    when(GoogleCloudStackdriverManager.getManager(anyString(),
                                             any(GoogleCloudCredentials.class),
                                             anyString(),
                                             anyString(),
                                             anyInt())).thenReturn(googleCloudStackdriverManager);
  }

  @Test
  public void testSettingCloudLoggingNotFromGce() throws Exception {
    LoggerFactory.getLogger("gcloud_stack_logging_not_from_gce");
    ArgumentCaptor<GoogleCloudCredentials> credentialsCaptor =
        ArgumentCaptor.forClass(GoogleCloudCredentials.class);

    verifyStatic();
    GoogleCloudStackdriverManager.getManager(eq("gcloud_stack_logging_not_from_gce"),
                                        credentialsCaptor.capture(),
                                        anyString(),
                                        anyString(),
                                        anyInt());
    assertFalse(credentialsCaptor.getValue().usingComputeCredentials());
    assertEquals("service2Id@developer.gserviceaccount.com",
                 credentialsCaptor.getValue().getServiceAccountId());
    assertEquals("file2.p12",
                 credentialsCaptor.getValue().getServiceAccountPrivateKeyP12FileName());
  }
}
