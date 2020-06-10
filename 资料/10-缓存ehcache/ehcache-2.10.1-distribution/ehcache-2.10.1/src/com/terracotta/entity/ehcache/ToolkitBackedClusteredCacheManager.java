/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity.ehcache;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.entity.ClusteredEntityState;
import com.terracotta.entity.EntityLockHandler;
import com.terracotta.entity.internal.InternalRootEntity;
import com.terracotta.entity.internal.LockingEntity;
import com.terracotta.entity.internal.ToolkitAwareEntity;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * ClusteredCacheManager
 */
public class ToolkitBackedClusteredCacheManager implements ClusteredCacheManager, ToolkitAwareEntity, LockingEntity,
                                                           InternalRootEntity {

  private static final long                        serialVersionUID = 1L;
  private static final String CACHE_ENTITY_MAP_PREFIX = "__entity_cache_root@";
  private static final String CACHE_ENTITY_LOCK_PREFIX = "__entity_cache_lock@";
  private static final long TRY_LOCK_TIMEOUT_SECONDS = 2;
  
  private final ClusteredCacheManagerConfiguration configuration;
  private final String cacheManagerName;
  private final ConcurrentMap<ToolkitObjectType, Set<String>> toolkitDSInfo;
  
  private volatile ClusteredEntityState state;

  private volatile transient Toolkit toolkit;
  private volatile transient EntityLockHandler entityLockHandler;
  private volatile transient ToolkitMap<String, ToolkitBackedClusteredCache> localCachesMap;

  public ToolkitBackedClusteredCacheManager(String cacheManagerName, ClusteredCacheManagerConfiguration configuration) {
    this.cacheManagerName = cacheManagerName;
    this.configuration = configuration;
    state = ClusteredEntityState.LIVE;
    this.toolkitDSInfo = new ConcurrentHashMap<ToolkitObjectType, Set<String>>();
    addCacheManagerMetaInfo(ToolkitObjectType.MAP, EhcacheEntitiesNaming.getCacheManagerConfigMapName(cacheManagerName));
  }

  @Override
  public ClusteredCacheManagerConfiguration getConfiguration() {
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
  public void setEntityLockHandler(EntityLockHandler entityLockHandler) {
    this.entityLockHandler = entityLockHandler;
  }

  @Override
  public Map<String, ClusteredCache> getCaches() {
    HashMap<String, ClusteredCache> resultMap = new HashMap<String, ClusteredCache>();
    for (Map.Entry<String, ToolkitBackedClusteredCache> cacheEntry : getCachesMap().entrySet()) {
      ToolkitBackedClusteredCache clusteredCache = cacheEntry.getValue();
      if (ClusteredEntityState.DESTROY_IN_PROGRESS.equals(clusteredCache.getState())) {
        destroyCacheSilently(clusteredCache);
      } else {
        resultMap.put(cacheEntry.getKey(), processEntry(clusteredCache));
      }
    }
    return Collections.unmodifiableMap(resultMap);
  }

  @Override
  public ClusteredCache getCache(String cacheName) {
    ToolkitBackedClusteredCache cache = getCacheInternal(cacheName);
    if (cache != null && ClusteredEntityState.DESTROY_IN_PROGRESS.equals(cache.getState())) {
      destroyCacheSilently(cache);
      return null;
    }
    return cache;
  }

  @Override
  public ClusteredCache addCacheIfAbsent(String cacheName, ClusteredCache clusteredCache) {
    ToolkitBackedClusteredCache tkClusteredCache = asToolkitClusteredCache(clusteredCache);
    return getCachesMap().putIfAbsent(cacheName, tkClusteredCache);
  }

  @Override
  public boolean destroyCache(ClusteredCache clusteredCache) {
    ToolkitLock entityReadLock = getEntityLock().readLock();
    try {
      if (entityReadLock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        try {
          ToolkitBackedClusteredCache tkClusteredCache = asToolkitClusteredCache(clusteredCache);
          if (!getCachesMap().containsKey(tkClusteredCache.getName())) {
            return false;
          }
          ToolkitLock writeLock = getCacheLock(tkClusteredCache.getName()).writeLock();
          if (writeLock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            try {
              ClusteredCache currentClusteredCache = getCacheInternal(tkClusteredCache.getName());
              if (!currentClusteredCache.equals(tkClusteredCache)) {
                throw new IllegalArgumentException(String.format("The specified clustered cache named %s does not match " +
                    "the mapping known to clustered cache manager named %s",tkClusteredCache.getName(),cacheManagerName));
              }
              tkClusteredCache.markDestroyInProgress();
              try {
                getCachesMap().put(tkClusteredCache.getName(), tkClusteredCache);
              } catch (Exception e) {
              // Failed to save entity in destroy state - abort
                tkClusteredCache.alive();
                throw new UnsupportedOperationException(String.format("Unable to mark cache %s with destroy in progress", tkClusteredCache.getName()), e);
              }
              processEntry(tkClusteredCache).destroy();
              getCachesMap().remove(clusteredCache.getName());
              return true;
            } finally {
              writeLock.unlock();
            }
          } else {
            throw new IllegalStateException(String.format("Unable to lock cache %s for destruction", clusteredCache.getName()));
          }
        } finally {
          entityReadLock.unlock();
        }
      } else {
        throw new IllegalStateException(String.format("Clustered cache manager %s is not allowing shared access", cacheManagerName));
      }
    } catch (InterruptedException e) {
      throw new IllegalStateException(String.format("Clustered cache manager %s is not allowing shared access", cacheManagerName), e);
    }
  }

  public void addCacheMetaInfo(String cacheName, ToolkitObjectType type, String dsName) {
    assertCacheExist(cacheName);
    ToolkitBackedClusteredCache tkClusteredCache = asToolkitClusteredCache(getCache(cacheName));
    if (tkClusteredCache.addToolkitDSMetaInfo(type, dsName)) {
      getCachesMap().put(tkClusteredCache.getName(), tkClusteredCache);
    }
  }

  public void addKeyRemoveInfo(String cacheName, String toolkitMapName, String keytoBeRemoved) {
    assertCacheExist(cacheName);
    ToolkitBackedClusteredCache tkClusteredCache = asToolkitClusteredCache(getCache(cacheName));
    if (tkClusteredCache.addKeyRemoveInfo(toolkitMapName, keytoBeRemoved)) {
      getCachesMap().put(tkClusteredCache.getName(), tkClusteredCache);
    }
  }

  @Override
  public ToolkitReadWriteLock getCacheLock(String cacheName) {
    return toolkit.getReadWriteLock(getCacheLockName(cacheName));
  }

  @Override
  public ToolkitReadWriteLock getEntityLock() {
    return toolkit.getReadWriteLock(EhcacheEntitiesNaming.getCacheManagerLockNameFor(cacheManagerName));
  }

  @Override
  public void markInUse() {
    entityLockHandler.readLock(EhcacheEntitiesNaming.getCacheManagerLockNameFor(cacheManagerName));
  }

  @Override
  public void releaseUse() {
    entityLockHandler.readUnlock(EhcacheEntitiesNaming.getCacheManagerLockNameFor(cacheManagerName));
  }

  @Override
  public boolean isUsed() {
    ToolkitLock entityWriteLock = getEntityLock().writeLock();
    if (entityWriteLock.tryLock())
      try {
        return false;
      } finally {
        entityWriteLock.unlock();
      }
    else {
      return true;
    }
  }

  @Override
  public void markCacheInUse(ClusteredCache clusteredCache) {
    entityLockHandler.readLock(getCacheLockName(clusteredCache.getName()));
  }

  @Override
  public void releaseCacheUse(ClusteredCache clusteredCache) {
    entityLockHandler.readUnlock(getCacheLockName(clusteredCache.getName()));
  }

  @Override
  public boolean isCacheUsed(ClusteredCache clusteredCache) {
    ClusteredCache currentClusteredCache = getCache(clusteredCache.getName());
    if (currentClusteredCache == null || !currentClusteredCache.equals(clusteredCache)) {
      throw new IllegalArgumentException(String.format("The specified clustered cache %s is not know to this clustered cache manager %s",
                                                       clusteredCache.getName(),
                                                       cacheManagerName));
    }
    ToolkitLock cacheWriteLock = getCacheLock(clusteredCache.getName()).writeLock();
    if (cacheWriteLock.tryLock()) {
      try {
        return false;
      } finally {
        cacheWriteLock.unlock();
      }
    } else {
      return true;
    }
  }

  @Override
  public void destroy() {
    for (ToolkitBackedClusteredCache clusteredCache : getCachesMap().values()) {
      processEntry(clusteredCache).destroy();
    }
    getCachesMap().destroy();

    // destroy all associated toolkit DS
    for (Entry<ToolkitObjectType, Set<String>> entry : toolkitDSInfo.entrySet()) {
      ToolkitObjectType type = entry.getKey();
      Set<String> values = entry.getValue();
      switch (type) {
        case MAP:
          for (String name : values) {
            toolkit.getMap(name, String.class, Serializable.class).destroy();
          }
          break;
        default:
          throw new IllegalStateException("got wrong ToolkitObjectType " + type);
      }
    }
  }

  @Override
  public void markDestroying() {
    state = ClusteredEntityState.DESTROY_IN_PROGRESS;
  }

  @Override
  public void alive() {
    state = ClusteredEntityState.LIVE;
  }

  String getCachesMapName() {
    return CACHE_ENTITY_MAP_PREFIX + cacheManagerName;
  }

  String getCacheLockName(String cacheName) {
    return CACHE_ENTITY_LOCK_PREFIX + cacheManagerName + "@" + cacheName;
  }

  private void destroyCacheSilently(ToolkitBackedClusteredCache clusteredCache) {
    try {
      destroyCache(clusteredCache);
    } catch (Exception e) {
      // Ignore - trying to destroy left overs
    }
  }

  private void addCacheManagerMetaInfo(ToolkitObjectType type, String dsName) {
    Set<String> tmpValues = new HashSet<String>();
    tmpValues.add(dsName);
    Set<String> oldValues = toolkitDSInfo.putIfAbsent(type, tmpValues);
    if (oldValues != null) {
      oldValues.add(dsName);
    }
  }

  private ToolkitBackedClusteredCache getCacheInternal(String cacheName) {
    return processEntry(getCachesMap().get(cacheName));
  }

  private void assertCacheExist(String cacheName) {
    if (!getCachesMap().containsKey(cacheName)) {
      throw new IllegalArgumentException(String.format("The specified clustered cache named %s does not match "+
          "the mapping known to clustered cache manager named %s", cacheName, cacheManagerName)); }
  }

  private ToolkitBackedClusteredCache asToolkitClusteredCache(ClusteredCache clusteredCache) {
    if (!(clusteredCache instanceof ToolkitBackedClusteredCache)) {
      throw new IllegalArgumentException("Unexpected implementation of ClusteredCache: " + clusteredCache.getClass());
    }
    return (ToolkitBackedClusteredCache)clusteredCache;
  }

  private ToolkitBackedClusteredCache processEntry(ToolkitBackedClusteredCache clusteredCache) {
    if (clusteredCache != null) {
      clusteredCache.setToolkit(toolkit);
    }
    return clusteredCache;
  }

  private ToolkitMap<String, ToolkitBackedClusteredCache> getCachesMap() {
      if (localCachesMap == null) {
          localCachesMap = toolkit.getMap(getCachesMapName(), String.class, ToolkitBackedClusteredCache.class);
      }
      return localCachesMap;
  }
}
