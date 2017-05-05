package io.imaravic.log4j.pubsub;

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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GoogleCloudPubsubManager.class})
@PowerMockIgnore({"javax.management.*", "javax.crypto.*"})
public class GoogleCloudPubsubIntegrationNotFromGceTest {
  @Before
  public void setup() throws Exception {
    mockStatic(GoogleCloudPubsubManager.class);
    final GoogleCloudPubsubManager googleCloudPubsubManager = mock(GoogleCloudPubsubManager.class);
    when(GoogleCloudPubsubManager.getManager(anyString(),
                                             any(GoogleCloudCredentials.class),
                                             anyString(),
                                             anyString(),
                                             anyBoolean(),
                                             anyInt())).thenReturn(googleCloudPubsubManager);
  }

  @Test
  public void testSettingCloudLoggingNotFromGce() throws Exception {
    LoggerFactory.getLogger("gcloud_logging_not_from_gce");
    ArgumentCaptor<GoogleCloudCredentials> credentialsCaptor =
        ArgumentCaptor.forClass(GoogleCloudCredentials.class);

    verifyStatic();
    GoogleCloudPubsubManager.getManager(eq("gcloud_logging_not_from_gce"),
                                        credentialsCaptor.capture(),
                                        eq("gcloud-projectId"),
                                        anyString(),
                                        eq(false),
                                        anyInt());
    assertFalse(credentialsCaptor.getValue().usingComputeCredentials());
    assertEquals("service1Id@developer.gserviceaccount.com",
                 credentialsCaptor.getValue().getServiceAccountId());
    assertEquals("file.p12",
                 credentialsCaptor.getValue().getServiceAccountPrivateKeyP12FileName());
  }
}
