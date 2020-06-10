package com.terracotta.entity.ehcache;

import com.terracotta.entity.ClusteredEntity;

/**
 * ClusteredCache
 */
public interface ClusteredCache extends ClusteredEntity<ClusteredCacheConfiguration> {
  String getName();

  long getSize();
}
