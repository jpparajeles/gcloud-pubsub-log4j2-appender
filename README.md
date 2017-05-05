Log4j2 Appender For Google Cloud Pubsub and Stackdriver Logging
===============================================================

A log4j2 appender to publish logs directly to [Google Cloud Pubsub](https://cloud.google.com/pubsub/docs/) or 
[Google Cloud Stacdriver Logging](https://cloud.google.com/logging/docs/).

All the calls to Google Cloud are blocking, 
so it's a good idea to combine this appender with AsyncAppender, or with AsyncLogger.

In case this appender is combined with either AsyncAppender or AsyncLogger it performs batching,
which is controlled from either AsyncAppender or AsyncLogger;

Usage
-----

### In `log4j2.xml`

If the Java Application is run from Google Cloud machine (Appengine, Dataflow or Compute), 
all the metadata, including credentials can be read from `metadata` service.
Please note, that the machine should be created with scope `pubsub` 
so it would be able to send logs.

In that case Appender config is as simple as possible.
```xml
<Appenders>
  <GoogleCloudPubsub  name="gcloud_logging_from_gce" 
                      topic="topic" 
                      autoCreateTopic="true"/>
  <GoogleCloudStackdriver   name="gcloud_stack_logging_from_gce"
                            resourceName="gce"
                            logName="projects/[PROJECT_ID]/logs/[LOG_ID]"/>
</Appenders>
```

In case the Java Application is not run from Google Cloud machine,
or if you just want to control all params manually and/or use ServiceAccount credentials
config is a bit more complicated.
```xml
<Appenders>
  <GoogleCloudPubsub  name="gcloud_logging_not_from_gce"
                      projectId="gcloud-projectId"
                      topic="topic"   
                      serviceAccountId="serviceId@developer.gserviceaccount.com"
                      serviceAccountPrivateKeyP12FileName="file.p12">
  </GoogleCloudPubsub>
  <GoogleCloudStackdriver   name="gcloud_stack_logging_not_from_gce"
                            resourceName="gce"
                            logName="projects/[PROJECT_ID]/logs/[LOG_ID]"
                            serviceAccountId="service2Id@developer.gserviceaccount.com"
                            serviceAccountPrivateKeyP12FileName="file2.p12"/>
</Appenders>
```

### In `pom.xml`

Artifact is still not uploaded to Maven Central.

To try it out, clone it and run 

```bash
mvn install
```

after this you can add following to your `pom.xml`

```xml
<dependency>
  <groupId>io.imaravic.log4j</groupId>
  <artifactId>google-cloud-log4j2-appender</artifactId>
  <version>0.0.3-SNAPSHOT</version>
</dependency>
```

Beside this log4j2 should be bootstraped for your application too.
