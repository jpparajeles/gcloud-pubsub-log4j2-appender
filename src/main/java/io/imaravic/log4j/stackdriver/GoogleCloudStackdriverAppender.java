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

import io.imaravic.log4j.util.GoogleCloudCredentials;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.validation.constraints.Required;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.Serializable;

/**
 * Created by josep on 3/5/2017.
 */


@Plugin(name = "GoogleCloudStackdriver", category = Node.CATEGORY, elementType = "appender", printObject = true)
public class GoogleCloudStackdriverAppender extends AbstractAppender {
  private static final long serialVersionUID = 1L;

  private final GoogleCloudStackdriverManager googleCloudStackdriverManager;

  public GoogleCloudStackdriverAppender(String name,
                                        Filter filter,
                                        Layout<? extends Serializable> layout,
                                        boolean ignoreExceptions,
                                        GoogleCloudStackdriverManager googleCloudStackdriverManager) {
    super(name, filter, layout, ignoreExceptions);
    this.googleCloudStackdriverManager = googleCloudStackdriverManager;
  }

  @Override
  public void append(LogEvent event) {
    googleCloudStackdriverManager.write(event, getLayout());
  }

  @PluginBuilderFactory
  public static GoogleCloudStackdriverAppender.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder implements org.apache.logging.log4j.core.util.Builder<GoogleCloudStackdriverAppender> {

    @PluginElement("Layout")
    private Layout<? extends Serializable> layout = PatternLayout.createDefaultLayout();

    @PluginElement("Filter")
    private Filter filter;

    @PluginBuilderAttribute
    private String serviceAccountId;

    @PluginBuilderAttribute
    private String serviceAccountPrivateKeyP12FileName;

    @PluginBuilderAttribute
    @Required
    private String name = "";

    @PluginBuilderAttribute
    private boolean ignoreExceptions = true;

    @PluginBuilderAttribute
    private int maxRetryTimeMillis = 500;

//    @PluginBuilderAttribute
//    private String projectId;

    @PluginBuilderAttribute
    @Required
    private String resourceName;

    @PluginBuilderAttribute
    @Required
    private String logName;


    @Override
    public GoogleCloudStackdriverAppender build() {
      try {

        GoogleCloudCredentials googleCloudCredentials =
            serviceAccountId == null
                ? GoogleCloudCredentials.newBuilder().withComputeCredentials(true).build()
                : GoogleCloudCredentials.createGoogleCloudCredentials(serviceAccountId, serviceAccountPrivateKeyP12FileName);

        return new GoogleCloudStackdriverAppender(name,
            filter,
            layout,
            ignoreExceptions,
            GoogleCloudStackdriverManager.getManager(name,
                googleCloudCredentials,
                resourceName,
                logName,
                maxRetryTimeMillis));
      } catch (final Throwable e) {
        LOGGER.error("Error creating GoogleCloudStackdriverAppender [{}]", name, e);
        return null;
      }
    }
  }


}
