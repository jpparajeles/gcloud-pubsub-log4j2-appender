/*
 * Copyright (c) 2017 José Pablo Parajeles
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

package io.imaravic.log4j.stackdriver;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Lists;
import com.google.api.services.logging.v2.Logging;
import com.google.api.services.logging.v2.LoggingScopes;
import com.google.api.services.logging.v2.model.LogEntry;
import com.google.api.services.logging.v2.model.MonitoredResource;
import com.google.api.services.logging.v2.model.WriteLogEntriesRequest;
import com.google.common.annotations.VisibleForTesting;
import io.imaravic.log4j.util.GoogleCloudCredentials;
import io.imaravic.log4j.util.RetryHttpInitializerWrapper;
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


/**
 * Created by josep on 3/5/2017.
 */



public class GoogleCloudStackdriverManager extends AbstractManager {
  private static final String APPLICATION_NAME = "GoogleCloudStackdriver-Log4j2Appender";


  private Logging stackdriverLoggingClient;
  private String monitoredResource;
  private String logName;
  private List<LogEntry> logsBuffer = Lists.newArrayList();
  
  @VisibleForTesting
  GoogleCloudStackdriverManager(final String name,
                                final HttpTransport transport,
                                final GoogleCloudCredentials googleCloudCredentials,
                                final String resourceName,
                                final String logName,
                                final int maxRetryTimeMillis)
      throws GeneralSecurityException, IOException
  {
    super(null, name);
    
    this.monitoredResource = resourceName;
    this.logName = logName;
    
    this.stackdriverLoggingClient = createStackdriverLoggingClient(transport,googleCloudCredentials,maxRetryTimeMillis);
    
  }
  
  private static String testInterception() {
    return "sorry not intercepted :(";
  }

  private static Logging createStackdriverLoggingClient(final HttpTransport transport,
                                                 final GoogleCloudCredentials googleCloudCredentials,
                                                 final int maxRetryTimeMillis)
      throws GeneralSecurityException, IOException {
    final JacksonFactory jacksonFactory = JacksonFactory.getDefaultInstance();
    Credential credential = googleCloudCredentials.getCredential(transport,
        jacksonFactory,
        LoggingScopes.all());
    
    return new Logging.Builder(transport,
        jacksonFactory,
        new RetryHttpInitializerWrapper(
            credential,
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
        writeToGoogleCloudLogging(writeLogEntriesRequest);
      }
      catch (IOException e){
        throw new AppenderLoggingException("Publishing message to Stackdriver Logging failed",e);
      }
    }
  }
  
  @VisibleForTesting
  void writeToGoogleCloudLogging(WriteLogEntriesRequest writeLogEntriesRequest) throws IOException {
    stackdriverLoggingClient.entries().write(writeLogEntriesRequest).execute();
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
              return new GoogleCloudStackdriverManager(name,
                  transport,
                  googleCloudCredentials,
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
