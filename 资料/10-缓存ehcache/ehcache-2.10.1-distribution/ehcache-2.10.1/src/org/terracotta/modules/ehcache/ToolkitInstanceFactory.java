/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;
import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheDisposalNotification;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.transaction.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;

import java.io.Serializable;
import java.util.Set;

/**
 * Factory used for creating {@link Toolkit} instances used for implementing clustered ehcache
 */
public interface ToolkitInstanceFactory {

  /**
   * Returns the toolkit associated with this factory
   */
  Toolkit getToolkit();

  /**
   * Returns a fully qualified name for the cache
   */
  String getFullyQualifiedCacheName(Ehcache cache);

  /**
   * Returns the backend {@link ToolkitCacheInternal} to be used for the cache
   */
  ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(Ehcache cache);

  /**
   * Returns a {@link ToolkitNotifier} for the cache for notifying {@link CacheConfigChangeNotificationMsg} across the
   * cluster
   */
  ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitReadWriteLock} for protecting the cache's store cluster wide
   */
  ToolkitLock getOrCreateStoreLock(Ehcache cache);

  ToolkitMap<String, AsyncConfig> getOrCreateAsyncConfigMap();

  ToolkitMap<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName, String cacheName);

  /**
   * Returns a {@link ToolkitNotifier} for the cachse to notify {@link CacheEventNotificationMsg} across the cluster
   */
  ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache);

  /**
   * Returns a {@link ToolkitMap} for storing serialized extractors for the cache
   * 
   * @throws UnsupportedOperationException if search is not supported
   * @param cacheManagerName
   * @param cacheName
   */
  ToolkitMap<String, AttributeExtractor> getOrCreateExtractorsMap(final String cacheManagerName, String cacheName);

  /**
   * Returns a {@link ToolkitMap} that will be used internally by Toolkit to store attribute schema.
   * @param cacheName
   *
   */
  ToolkitMap<String, String> getOrCreateAttributeMap(final String cacheManagerName, String cacheName);

  /**
   * Destorys any clustered state associated with the given cache.
   *
   * @param cacheManagerName
   * @param cacheName
   */
  boolean destroy(final String cacheManagerName, final String cacheName);

  /**
   * Shutdown
   */
  void shutdown();

  /**
   * Return the map used for storing commit state of ehcache transactions
   */
  SerializedToolkitCache<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName);

  SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> getOrCreateAllSoftLockMap(String cacheManagerName,
                                                                                                                     String cacheName);

  ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                  String cacheName);

  ToolkitMap<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName);

  ToolkitMap<String, Serializable> getOrCreateCacheManagerMetaInfoMap(String cacheManagerName);

  ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID, Object key);

  ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                             Object key);

  ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                               Object key);

  void removeNonStopConfigforCache(Ehcache cache);

  ToolkitLock getLockForCache(Ehcache cache, String lockName);

  ToolkitNotifier<CacheDisposalNotification> getOrCreateCacheDisposalNotifier(Ehcache cache);

  /**
   * This method should only be used by the Orchestrator for fetching a {@link WanAwareToolkitCache}
   */
  WanAwareToolkitCache<String, Serializable> getOrCreateWanAwareToolkitCache(String cacheManagerName,
                                                                             String cacheName,
                                                                             CacheConfiguration ehcacheConfig,
                                                                             boolean masterCache,
                                                                             boolean bidirectional);

  void waitForOrchestrator(String cacheManagerName);

  void markCacheWanDisabled(String cacheManagerName, String cacheName);

  /**
   * Links a terracotta enabled cache manager to the cluster
   *
   * @param cacheManagerName the cache manager name
   * @param configuration
   */
  void linkClusteredCacheManager(String cacheManagerName, Configuration configuration);

  /**
   * Un-links a terracotta enabled cache from the cluster
   *
   * @param cacheName the cache name
   */
  void unlinkCache(String cacheName);

  ToolkitListInternal getAsyncProcessingBucket(String bucketName, String cacheName);

  void clusterRejoined();
}
