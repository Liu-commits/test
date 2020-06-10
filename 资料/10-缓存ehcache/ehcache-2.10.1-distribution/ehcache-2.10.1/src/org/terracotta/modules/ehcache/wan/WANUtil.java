/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.wan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.ToolkitInstanceFactory;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;

import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class WANUtil {
  private static final int             WAIT_INTERVAL_FOR_ORCHESTRATOR_IN_SECONDS = 60;
  private static final Logger          LOGGER                                    = LoggerFactory.getLogger(WANUtil.class);
  private static final String          WAN_PREFIX                                = "__WAN__";
  private static final String          LOCK_PREFIX                               = WAN_PREFIX + "LOCK";
  private static final String          WAN_ENABLED_CACHE_ENTRY                   = WAN_PREFIX + "ENABLED_CACHE";
  private static final String          REPLICA_CACHE_FLAG                        = "IS_REPLICA";
  private static final String          META_DATA_AVAILABLE_FLAG                  = "WAN_META_DATA_AVAILABLE";
  private static final String          BIDIRECTIONAL_FLAG                        = "IS_BIDIRECTIONAL";
  private static final String          WAN_CURRENT_ORCHESTRATOR                  = WAN_PREFIX + "CURRENT_ORCHESTRATOR";

  private final ToolkitInstanceFactory factory;

  public WANUtil(ToolkitInstanceFactory factory) {
    this.factory = factory;
  }

  /**
   * This method marks that WAN meta-data is available for the given cacheManager
   * 
   * @param cacheManagerName name of the CacheManager
   */
  public void markWANReady(String cacheManagerName) {
    getCacheManagerConfigMap(cacheManagerName).put(META_DATA_AVAILABLE_FLAG, Boolean.TRUE);
    notifyClients(cacheManagerName);
  }

  /**
   * This method clears the WAN meta-data available status for the given cacheManager
   * 
   * @param cacheManagerName name of the CacheManager
   */
  public void clearWANReady(String cacheManagerName) {
    getCacheManagerConfigMap(cacheManagerName).put(META_DATA_AVAILABLE_FLAG, Boolean.FALSE);
  }

  /**
   * This method is used to check whether the WAN meta-data is available or not.
   * 
   * @param cacheManagerName
   * @return <code>true</code> if meta-data is available else <code>false</code>.
   */
  public boolean isWANReady(String cacheManagerName) {
    Boolean value = (Boolean) getCacheManagerConfigMap(cacheManagerName).get(META_DATA_AVAILABLE_FLAG);
    return (value == null) ? false : value;
  }

  /**
   * This method is used to wait until the Orchestrator is running.
   * 
   * @param cacheManagerName
   */
  public void waitForOrchestrator(String cacheManagerName) {
    if (!isWANReady(cacheManagerName)) {
      LOGGER.info("Waiting for the Orchestrator...");
      ToolkitLock toolkitLock = factory.getToolkit().getLock(LOCK_PREFIX + cacheManagerName);
      toolkitLock.lock();
      try {
        while (!isWANReady(cacheManagerName)) {
          try {
            boolean orchRunning = toolkitLock.getCondition().await(WAIT_INTERVAL_FOR_ORCHESTRATOR_IN_SECONDS,
                                                                 TimeUnit.SECONDS);
            if (!orchRunning) {
              LOGGER.error("No Orchestrator Running. We can not proceed further without an Orchestrator.");
            }
          } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for the Orchestrator to be running.", e);
          }
        }
      } finally {
        toolkitLock.unlock();
      }
    }

    LOGGER.info("Orchestrator is available for the CacheManager '{}'", cacheManagerName);
  }

  /**
   * This method is used by Orchestrator to mark the cache as wan-enabled.
   * 
   * @param cacheManagerName
   * @param cacheName
   * @throws IllegalConfigurationException if the cache is already marked as wan disabled
   */
  public void markCacheWanEnabled(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    final Boolean existingValue = (Boolean) cacheConfigMap.putIfAbsent(WAN_ENABLED_CACHE_ENTRY, Boolean.TRUE);
    if ((existingValue != null) && (existingValue.equals(Boolean.FALSE))) {
      LOGGER.error("A Client with cache '{}' exists with non WAN configuration. "
                   + "Please check your client's ehcache.xml and add 'wanEnabledTSA = true'", cacheName);
      throw new IllegalConfigurationException("Cache '" + cacheName + "' is already marked as disabled for WAN");
    }
    LOGGER.info("Marked the cache '{}' wan enabled for CacheManager '{}'", cacheName, cacheManagerName);
  }

  public void markCacheAsReplica(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.put(REPLICA_CACHE_FLAG, Boolean.TRUE);
    LOGGER.info("Cache '{}' in CacheManager '{}' has been marked as a Replica", cacheName, cacheManagerName);
  }

  public void markCacheAsMaster(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.put(REPLICA_CACHE_FLAG, Boolean.FALSE);
    LOGGER.info("Cache '{}' in CacheManager '{}' has been marked as a Master", cacheName, cacheManagerName);
  }

  public boolean isCacheReplica(String cacheManagerName, String cacheName) {
    if (cacheName == null || cacheManagerName == null) {
      throw new IllegalArgumentException("Invalid arguments: CacheManagerName- " + cacheManagerName
                                         + " and CacheName- " + cacheName);
    }

    return (Boolean) getCacheConfigMap(cacheManagerName, cacheName).get(REPLICA_CACHE_FLAG);
  }

  public void markCacheAsBidirectional(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.put(BIDIRECTIONAL_FLAG, Boolean.TRUE);
    LOGGER.info("Cache '{}' in CacheManager '{}' has been marked as BIDIRECTIONAL", cacheName, cacheManagerName);
  }

  public void markCacheAsUnidirectional(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.put(BIDIRECTIONAL_FLAG, Boolean.FALSE);
    LOGGER.info("Cache '{}' in CacheManager '{}' has been marked as UNIDIRECTIONAL", cacheName, cacheManagerName);
  }

  public void addCurrentOrchestrator(String cacheManagerName, String cacheName, String orchestrator) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.put(WAN_CURRENT_ORCHESTRATOR, orchestrator);
    LOGGER.info("Added '{}' as orchestrator for Cache '{}' in CacheManager '{}'", orchestrator, cacheName, cacheManagerName);
  }

  public boolean isCacheBidirectional(String cacheManagerName, String cacheName) {
    if (cacheName == null || cacheManagerName == null) {
      throw new IllegalArgumentException("Invalid arguments: CacheManagerName- " + cacheManagerName
                                         + " and CacheName- " + cacheName);
    }

    return (Boolean) getCacheConfigMap(cacheManagerName, cacheName).get(BIDIRECTIONAL_FLAG);
  }

  /**
   * This method is used by Client to mark the cache as wan-disabled.
   * 
   * @param cacheName
   * @throws IllegalConfigurationException if the cache is already marked as wan enabled
   */
  public void markCacheWanDisabled(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    final Boolean existingValue = (Boolean) cacheConfigMap.putIfAbsent(WAN_ENABLED_CACHE_ENTRY, Boolean.FALSE);
    if ((existingValue != null) && (existingValue.equals(Boolean.TRUE))) {
      LOGGER.error("A WAN Orchestrator already exists for cache '{}'. This client should be wan-enabled. "
                   + "Please check your client's ehcache.xml and add 'wanEnabledTSA = true'", cacheName);
      throw new IllegalConfigurationException("Cache '" + cacheName + "' is already marked as enabled for WAN");
    }
    LOGGER.debug("Marked the cache '{}' wan disabled for CacheManager '{}'", cacheName, cacheManagerName);
  }

  /**
   * This method returns true if the cache is wan-enabled else false. This method should only be used by Client and not
   * by the Orchestrator.
   * 
   * @param cacheManagerName name of the CacheManager
   * @param cacheName name of the Cache
   */
  public boolean isWanEnabledCache(String cacheManagerName, String cacheName) {
    if (cacheName == null || cacheManagerName == null) {
      throw new IllegalArgumentException("Invalid arguments: CacheManagerName- " + cacheManagerName
                                         + " and CacheName- " + cacheName);
    }

    Boolean value = (Boolean) getCacheConfigMap(cacheManagerName, cacheName).get(WAN_ENABLED_CACHE_ENTRY);
    return (value == null) ? false : value;
  }

  /**
   * This method is used by clean-up scripts to clean the Cache status for WAN.
   * 
   * @param cacheManagerName name of the CacheManager
   * @param cacheName name of the Cache
   */
  public void cleanUpCacheMetaData(String cacheManagerName, String cacheName) {
    final ConcurrentMap<String, Serializable> cacheConfigMap = getCacheConfigMap(cacheManagerName, cacheName);
    cacheConfigMap.remove(WAN_ENABLED_CACHE_ENTRY);
    cacheConfigMap.remove(REPLICA_CACHE_FLAG);

    LOGGER.info("Cleaned up the metadata for cache '{}' for CacheManager '{}'", cacheName, cacheManagerName);
  }


  void notifyClients(String cacheManagerName) {
    ToolkitLock toolkitLock = factory.getToolkit().getLock(LOCK_PREFIX + cacheManagerName);
    toolkitLock.lock();
    try {
      toolkitLock.getCondition().signalAll();
    } finally {
      toolkitLock.unlock();
    }
  }

  ConcurrentMap<String, Serializable> getCacheConfigMap(String cacheManagerName, String cacheName) {
    return factory.getOrCreateClusteredStoreConfigMap(cacheManagerName, cacheName);
  }

  ConcurrentMap<String, Serializable> getCacheManagerConfigMap(String cacheManagerName) {
    return factory.getOrCreateCacheManagerMetaInfoMap(cacheManagerName);
  }

}
