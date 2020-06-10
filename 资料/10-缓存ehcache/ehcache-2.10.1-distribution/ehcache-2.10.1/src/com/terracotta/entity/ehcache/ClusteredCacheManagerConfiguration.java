/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity.ehcache;

import com.terracotta.entity.EntityConfiguration;

/**
 * ClusteredCacheManagerConfiguration
 */
public class ClusteredCacheManagerConfiguration implements EntityConfiguration {

  private static final long serialVersionUID = 1L;
  private final String      configurationText;

  public ClusteredCacheManagerConfiguration(String configurationText) {
    this.configurationText = configurationText;
  }

  public String getConfigurationAsText() {
    return configurationText;
  }
}
