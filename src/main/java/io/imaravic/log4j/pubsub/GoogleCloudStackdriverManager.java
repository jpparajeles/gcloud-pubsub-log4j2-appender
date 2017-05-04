package io.imaravic.log4j.pubsub;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.LoggingScopes;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.api.services.logging.v2.model.MonitoredResource;
import com.google.api.services.logging.v2.model.WriteLogEntriesRequest;
import com.google.common.collect.Lists;
import io.imaravic.log4j.pubsub.util.GoogleCloudMetadata;
import io.imaravic.log4j.pubsub.util.RetryHttpInitializerWrapper;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractManager;
import org.apache.logging.log4j.core.appender.AppenderLoggingException;
import org.apache.logging.log4j.core.appender.ManagerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.security.GeneralSecurityException;
import java.util.List;

//import com.google.api.services.logging.v2.

/**
 * Created by josep on 3/5/2017.
 */



public class GoogleCloudStackdriverManager extends AbstractManager {
  private static final String APPLICATION_NAME = "GoogleCloudStackdriver-Log4j2Appender";


  Logging stackdriverLoggingClient;
  String monitoredResource;
  String logName;
  List<LogEntry> logsBuffer = Lists.newArrayList();

  public GoogleCloudStackdriverManager(final String name,
                                       final HttpTransport transport,
                                       final GoogleCloudMetadata googleCloudMetadata,
                                       final GoogleCloudCredentials googleCloudCredentials,
                                       final String googleCloudProjectId,
                                       final String resourceName,
                                       final String logName,
                                       final int maxRetryTimeMillis)
      throws GeneralSecurityException, IOException
  {
    super(null, name);
    this.monitoredResource = resourceName;
    this.logName = logName;


    this.stackdriverLoggingClient = createStackDriverLoggingClient(transport,googleCloudCredentials,maxRetryTimeMillis);
  }

  private Logging createStackDriverLoggingClient(HttpTransport transport, GoogleCloudCredentials googleCloudCredentials,
                                                 int maxRetryTimeMillis) throws GeneralSecurityException, IOException {
    final JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
    return new Logging.Builder(transport,
        jacksonFactory,
        new RetryHttpInitializerWrapper(
            googleCloudCredentials.getCredential(transport,
                jacksonFactory,
                LoggingScopes.all()),
            maxRetryTimeMillis
        )).setApplicationName(APPLICATION_NAME).build();
  }


  public synchronized void write(final LogEvent event, Layout<? extends Serializable> layout) {
    final String logMsg = event.getMessage().getFormattedMessage();
    byte[] bytes = layout.toByteArray(event);

    LogEntry logEntry = new LogEntry();
    logEntry.setTextPayload(new String(bytes));
    Level level = event.getLevel();
    logEntry.setSeverity(translate(level));

    logsBuffer.add(logEntry);

    if (event.isEndOfBatch()){
      List<LogEntry> logsToWrite = this.logsBuffer;
      this.logsBuffer = Lists.newArrayList();

      WriteLogEntriesRequest writeLogEntriesRequest = new WriteLogEntriesRequest();
      writeLogEntriesRequest.setEntries(logsToWrite);
      MonitoredResource monitoredResource = new MonitoredResource().setType(this.monitoredResource);
      writeLogEntriesRequest.setLogName(this.logName).setResource(monitoredResource);

      try {
        stackdriverLoggingClient.entries().write(writeLogEntriesRequest).execute();
      }
      catch (IOException e){
        throw new AppenderLoggingException("Publishing message to Stackdriver Logging failed",e);
      }
    }
  }

  private String translate(Level level){
    if (level.isLessSpecificThan(Level.DEBUG)){
      return "DEBUG";
    }
    if (level.isLessSpecificThan(Level.INFO)){
      return "INFO";
    }
    if (level.isLessSpecificThan(Level.WARN)){
      return "WARNING";
    }
    if (level.isLessSpecificThan(Level.ERROR)){
      return "ERROR";
    }
    return "CRITICAL";
  }

  public static GoogleCloudStackdriverManager getManager(final String name,
                                                         final GoogleCloudCredentials googleCloudCredentials,
                                                         final String googleCloudProjectId,
                                                         final String resourceName,
                                                         final String logName,
                                                         final int maxRetryTimeMillis){
    return AbstractManager.getManager(
        name,
        new ManagerFactory<GoogleCloudStackdriverManager, Object>() {
          @Override
          public GoogleCloudStackdriverManager createManager(String name, Object data) {
            try{
              final HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
              final GoogleCloudMetadata googleCloudMetadata = new GoogleCloudMetadata(transport);
              return new GoogleCloudStackdriverManager(name,
                  transport,
                  googleCloudMetadata,
                  googleCloudCredentials,
                  googleCloudProjectId,
                  resourceName,
                  logName,
                  maxRetryTimeMillis);
            } catch (Throwable e){
              LOGGER.error("Failed to initialize GoogleCloudLoggingManager", e);
            }
            return null;
          }

        },null);

  }


}
