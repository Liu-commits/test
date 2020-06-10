/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity.ehcache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;

import com.terracotta.entity.ClusteredEntityState;
import com.terracotta.entity.internal.ToolkitAwareEntity;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ToolkitBackedClusteredCache implements ToolkitAwareEntity, ClusteredCache {

//  private static final Logger                      LOGGER                   = Logger.getLogger(ToolkitBackedClusteredCache.class.getName());
  private static final long                 serialVersionUID = 1L;
  private final String                      cacheName;
  private final String                      toolkitCacheName;
  private final ClusteredCacheConfiguration configuration;
  private final ConcurrentMap<ToolkitObjectType, Set<String>> toolkitDSInfo;
  private final ConcurrentMap<String, Set<String>>            keyRemoveInfo;

  private volatile ClusteredEntityState     state            = ClusteredEntityState.LIVE;

  private volatile transient Toolkit        toolkit;

  public ToolkitBackedClusteredCache(String cacheName, ClusteredCacheConfiguration configuration,
                                     String toolkitCacheName) {
    this.cacheName = cacheName;
    this.toolkitCacheName = toolkitCacheName;
    this.configuration = configuration;
    this.toolkitDSInfo = new ConcurrentHashMap<ToolkitObjectType, Set<String>>();
    this.keyRemoveInfo = new ConcurrentHashMap<String, Set<String>>();
  }

  @Override
  public ClusteredCacheConfiguration getConfiguration() {
    return configuration;
  }

  @Override
  public ClusteredEntityState getState() {
    return state;
  }

  @Override
  public void setToolkit(Toolkit toolkit) {
    this.toolkit = toolkit;
  }

  @Override
  public String getName() {
    return cacheName;
  }

  @Override
  public long getSize() {
    return ((ToolkitCacheInternal)toolkit.getCache(toolkitCacheName, Serializable.class)).quickSize();
  }

  public void destroy() {
    // destroy all associated toolkit DS
    for (Entry<ToolkitObjectType, Set<String>> entry : toolkitDSInfo.entrySet()) {
      ToolkitObjectType type = entry.getKey();
      Set<String> values = entry.getValue();
      switch (type) {
        case LIST:
          for (String name : values) {
            toolkit.getList(name, Serializable.class).destroy();
          }
          break;
        case MAP:
          for (String name : values) {
            toolkit.getMap(name, String.class, Serializable.class).destroy();
          }
          break;

        case CACHE:
          for (String name : values) {
            toolkit.getCache(name, Serializable.class).destroy();
          }
          break;

        case NOTIFIER:
          for (String name : values) {
            toolkit.getNotifier(name, Serializable.class).destroy();
          }
          break;

        default:
          throw new IllegalStateException("got wrong ToolkitObjectType " + type);
      }
    }

    // remove keys from toolkit maps
    for (Entry<String, Set<String>> entry : keyRemoveInfo.entrySet()) {
      String toolkitMapName = entry.getKey();
      Set<String> values = entry.getValue();
      ToolkitMap<String, Serializable> toolkitMap = toolkit.getMap(toolkitMapName, String.class, Serializable.class);
      for (String key : values) {
        toolkitMap.remove(key);
      }
    }

    // destroy toolkit cache
    toolkit.getCache(toolkitCacheName, new ToolkitCacheConfigBuilder().localCacheEnabled(false).offheapEnabled(false).build(),
                     Serializable.class).destroy();
  }

  public boolean addToolkitDSMetaInfo(ToolkitObjectType type, String dsName) {
    assertCacheAlive();
    Set<String> tmpValues = new HashSet<String>();
    tmpValues.add(dsName);
    Set<String> oldValues = toolkitDSInfo.putIfAbsent(type, tmpValues);
    if (oldValues != null) {
      return oldValues.add(dsName);
    }
    return true;
  }

  public boolean addKeyRemoveInfo(String toolkitMapName, String keytoBeRemoved) {
    assertCacheAlive();
    Set<String> tmpValues = new HashSet<String>();
    tmpValues.add(keytoBeRemoved);
    Set<String> oldValues = keyRemoveInfo.putIfAbsent(toolkitMapName, tmpValues);
    if (oldValues != null) {
      return oldValues.add(keytoBeRemoved);
    }
    return true;
  }

  private void assertCacheAlive() {
    if (state != ClusteredEntityState.LIVE) { 
      throw new IllegalStateException(String.format("cache %s state is %s",cacheName, state)); 
    }
  }

  public void markDestroyInProgress() {
    this.state = ClusteredEntityState.DESTROY_IN_PROGRESS;
  }

  public void alive() {
    this.state = ClusteredEntityState.LIVE;
  }
}
