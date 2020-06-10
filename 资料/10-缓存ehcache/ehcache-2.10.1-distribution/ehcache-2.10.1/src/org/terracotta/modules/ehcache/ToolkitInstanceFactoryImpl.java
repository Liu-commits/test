/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.ConfigurationFactory;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration.Consistency;
import net.sf.ehcache.config.generator.ConfigurationUtil;
import net.sf.ehcache.search.attribute.AttributeExtractor;
import net.sf.ehcache.transaction.Decision;
import net.sf.ehcache.transaction.TransactionID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.async.AsyncConfig;
import org.terracotta.modules.ehcache.collections.SerializationHelper;
import org.terracotta.modules.ehcache.collections.SerializedToolkitCache;
import org.terracotta.modules.ehcache.event.CacheDisposalNotification;
import org.terracotta.modules.ehcache.event.CacheEventNotificationMsg;
import org.terracotta.modules.ehcache.store.CacheConfigChangeNotificationMsg;
import org.terracotta.modules.ehcache.store.TerracottaClusteredInstanceFactory;
import org.terracotta.modules.ehcache.store.ToolkitNonStopConfiguration;
import org.terracotta.modules.ehcache.store.nonstop.ToolkitNonstopDisableConfig;
import org.terracotta.modules.ehcache.transaction.ClusteredSoftLockIDKey;
import org.terracotta.modules.ehcache.transaction.SerializedReadCommittedClusteredSoftLock;
import org.terracotta.modules.ehcache.wan.WANUtil;
import org.terracotta.modules.ehcache.wan.Watchable;
import org.terracotta.modules.ehcache.wan.Watchdog;
import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.ToolkitFeatureType;
import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.builder.ToolkitCacheConfigBuilder;
import org.terracotta.toolkit.builder.ToolkitStoreConfigBuilder;
import org.terracotta.toolkit.cache.ToolkitCache;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.events.ToolkitNotifier;
import org.terracotta.toolkit.internal.ToolkitInternal;
import org.terracotta.toolkit.internal.ToolkitLogger;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.collections.ToolkitListInternal;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.nonstop.NonStopConfigurationRegistry;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import com.terracotta.entity.ClusteredEntityManager;
import com.terracotta.entity.ehcache.ClusteredCache;
import com.terracotta.entity.ehcache.ClusteredCacheConfiguration;
import com.terracotta.entity.ehcache.ClusteredCacheManager;
import com.terracotta.entity.ehcache.ClusteredCacheManagerConfiguration;
import com.terracotta.entity.ehcache.EhcacheEntitiesNaming;
import com.terracotta.entity.ehcache.ToolkitBackedClusteredCache;
import com.terracotta.entity.ehcache.ToolkitBackedClusteredCacheManager;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

public class ToolkitInstanceFactoryImpl implements ToolkitInstanceFactory {

  public static final Logger           LOGGER                                   = LoggerFactory
                                                                                      .getLogger(ToolkitInstanceFactoryImpl.class);
  private static final String CONFIG_LOGGER_NAME = "com.terracotta.ehcache.config";

  public static final String  DELIMITER                                = "|";
  private static final String EVENT_NOTIFIER_SUFFIX                    = "event-notifier";
  private static final String DISPOSAL_NOTIFIER_SUFFIX                 = "disposal-notifier";
  private static final String EHCACHE_NAME_PREFIX                      = "__tc_clustered-ehcache";
  private static final String CONFIG_NOTIFIER_SUFFIX                   = "config-notifier";
  private static final String EHCACHE_TXNS_DECISION_STATE_MAP_NAME     = EHCACHE_NAME_PREFIX + DELIMITER
                                                                                    + "txnsDecision";
  private static final String ALL_SOFT_LOCKS_MAP_SUFFIX                = "softLocks";
  private static final String NEW_SOFT_LOCKS_LIST_SUFFIX               = "newSoftLocks";

  private static final String LOCK_TAG                                 = "::LOCK";

  static final String         CLUSTERED_STORE_CONFIG_MAP               = EHCACHE_NAME_PREFIX + DELIMITER + "configMap";

  private static final String EHCACHE_TXNS_SOFTLOCK_WRITE_LOCK_NAME    = EHCACHE_NAME_PREFIX + DELIMITER
                                                                                    + "softWriteLock";

  private static final String EHCACHE_TXNS_SOFTLOCK_FREEZE_LOCK_NAME   = EHCACHE_NAME_PREFIX + DELIMITER
                                                                                    + "softFreezeLock";
  private static final String EHCACHE_TXNS_SOFTLOCK_NOTIFIER_LOCK_NAME = EHCACHE_NAME_PREFIX + DELIMITER
                                                                                    + "softNotifierLock";
  public static final int RETRY_MARK_IN_USE_AFTER_REJOIN = 5;

  protected final Toolkit                 toolkit;
  private final WANUtil                   wanUtil;
  private final ClusteredEntityManager    clusteredEntityManager;
  private volatile ClusteredCacheManager  clusteredCacheManagerEntity;
  private final EntityNamesHolder         entityNames;
  private final Watchdog                  wanWatchdog;

  public ToolkitInstanceFactoryImpl(final TerracottaClientConfiguration terracottaClientConfiguration,
                                    final String productId, ClassLoader loader) {
    this.toolkit = createTerracottaToolkit(terracottaClientConfiguration, productId, loader);
    updateDefaultNonStopConfig(toolkit);
    this.clusteredEntityManager = new ClusteredEntityManager(toolkit);
    this.entityNames = new EntityNamesHolder();
    this.wanUtil = new WANUtil(this);
    this.wanWatchdog = Watchdog.create();
  }

  public ToolkitInstanceFactoryImpl(final TerracottaClientConfiguration terracottaClientConfiguration,
                                    ClassLoader loader) {
    this(terracottaClientConfiguration, null, loader);
  }

  // Constructor to enable unit testing
  ToolkitInstanceFactoryImpl(final Toolkit toolkit, final ClusteredEntityManager clusteredEntityManager) {
    this.toolkit = toolkit;
    this.clusteredEntityManager = clusteredEntityManager;
    this.entityNames = new EntityNamesHolder();
    this.wanUtil = new WANUtil(this);
    this.wanWatchdog = Watchdog.create();
  }

  ToolkitInstanceFactoryImpl(final Toolkit toolkit, final ClusteredEntityManager clusteredEntityManager,
                             final WANUtil util, final Watchdog wanWatchdog) {
    this.toolkit = toolkit;
    this.clusteredEntityManager = clusteredEntityManager;
    this.entityNames = new EntityNamesHolder();
    this.wanUtil = util;
    this.wanWatchdog = wanWatchdog;
  }

  private void updateDefaultNonStopConfig(Toolkit toolkitParam) {
    ToolkitNonstopDisableConfig disableNonStop = new ToolkitNonstopDisableConfig();
    NonStopConfigurationRegistry nonStopConfigurationRegistry = toolkitParam.getFeature(ToolkitFeatureType.NONSTOP)
        .getNonStopConfigurationRegistry();
    for (ToolkitObjectType t : ToolkitObjectType.values()) {
      try {
        nonStopConfigurationRegistry.registerForType(disableNonStop, t);
      } catch (UnsupportedOperationException e) {
        // expected for Barrier and BlockingQueue.
        if (!(t == ToolkitObjectType.BARRIER || t == ToolkitObjectType.BLOCKING_QUEUE)) { throw e; }
      }
    }
  }

  private static Toolkit createTerracottaToolkit(TerracottaClientConfiguration terracottaClientConfiguration,
                                                 String productId, ClassLoader loader) {
    TerracottaToolkitBuilder terracottaClientBuilder = new TerracottaToolkitBuilder();
    EhcacheTcConfig ehcacheTcConfig = EhcacheTcConfig.create(terracottaClientConfiguration);
    switch (ehcacheTcConfig.type) {
      case URL:
        terracottaClientBuilder.setTCConfigUrl(ehcacheTcConfig.tcConfigUrlOrSnippet);
        break;
      case EMBEDDED_TC_CONFIG:
      case FILE:
        terracottaClientBuilder.setTCConfigSnippet(ehcacheTcConfig.tcConfigUrlOrSnippet);
        break;
    }
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache");
    terracottaClientBuilder.addTunnelledMBeanDomain("net.sf.ehcache.hibernate");
    terracottaClientBuilder.addTunnelledMBeanDomain("org.terracotta.wan");
    terracottaClientBuilder.setRejoinEnabled(terracottaClientConfiguration.isRejoin());
    terracottaClientBuilder.setProductId(productId);
    terracottaClientBuilder.setClassLoader(loader);
    return terracottaClientBuilder.buildToolkit();
  }

  @Override
  public void waitForOrchestrator(String cacheManagerName) {
    wanUtil.waitForOrchestrator(cacheManagerName);
  }

  @Override
  public void markCacheWanDisabled(String cacheManagerName, String cacheName) {
    wanUtil.markCacheWanDisabled(cacheManagerName, cacheName);
  }

  @Override
  public Toolkit getToolkit() {
    return toolkit;
  }

  @Override
  public ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(final Ehcache cache) {
    final String cacheManagerName = getCacheManagerName(cache);
    final String cacheName = cache.getName();
    
    ToolkitCacheInternal<String, Serializable> toolkitCache = getOrCreateRegularToolkitCache(
        cacheManagerName, cacheName, cache.getCacheConfiguration());
    if(wanUtil.isWanEnabledCache(cacheManagerName, cacheName)) {
      final boolean replicaCache = wanUtil.isCacheReplica(cacheManagerName, cacheName);
      final boolean bidirectional = wanUtil.isCacheBidirectional(cacheManagerName, cacheName);
      toolkitCache = createWanAwareToolkitCache(cacheManagerName, cacheName, toolkitCache,
                                                cache.getCacheConfiguration(), !replicaCache, bidirectional);

      if (replicaCache) {
        LOGGER.info("Pinning the Cache '{}' belonging to Cache Manager '{}' " +
                    "and setting its TTI and TTL values to zero as it is a WAN Replica Cache. " +
                    "This cache's capacity will be controlled by its Master cache.",
            cacheName, cacheManagerName);
        PinningConfiguration pinningConfiguration = new PinningConfiguration();
        pinningConfiguration.setStore(PinningConfiguration.Store.INCACHE.toString());
        cache.getCacheConfiguration().addPinning(pinningConfiguration);
        cache.getCacheConfiguration().setMaxEntriesInCache(0);
        cache.getCacheConfiguration().setTimeToLiveSeconds(0);
        cache.getCacheConfiguration().setTimeToIdleSeconds(0);
      }

      cache.getCacheConfiguration().freezeConfiguration();

      wanWatchdog.watch((Watchable) toolkitCache);
    }

    return toolkitCache;
  }

  @Override
  public WanAwareToolkitCache<String, Serializable> getOrCreateWanAwareToolkitCache(final String cacheManagerName,
                                                                                    final String cacheName,
                                                                                    final CacheConfiguration ehcacheConfig,
                                                                                    final boolean masterCache,
                                                                                    final boolean bidirectional) {
    final ToolkitCacheInternal<String, Serializable> toolkitCache =
        getOrCreateRegularToolkitCache(cacheManagerName, cacheName, ehcacheConfig);
    return createWanAwareToolkitCache(cacheManagerName, cacheName, toolkitCache, ehcacheConfig, masterCache, bidirectional);
  }

  private WanAwareToolkitCache<String, Serializable> createWanAwareToolkitCache(final String cacheManagerName,
                                                                                final String cacheName,
                                                                                final ToolkitCacheInternal<String, Serializable> toolkitCache,
                                                                                final CacheConfiguration cacheConfiguration,
                                                                                final boolean masterCache,
                                                                                final boolean bidirectional) {
    final String fullyQualifiedCacheName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName);
    final ToolkitMap<String, Serializable> configMap = getOrCreateConfigMap(fullyQualifiedCacheName);
    return new WanAwareToolkitCache<String, Serializable>((BufferingToolkitCache<String,Serializable>)toolkitCache, configMap,
                                                          toolkit.getFeature(ToolkitFeatureType.NONSTOP),
                                                          toolkit.getLock(toolkitCache.getName() + LOCK_TAG),
                                                          cacheConfiguration, masterCache, bidirectional);
  }

  private ToolkitCacheInternal<String, Serializable> getOrCreateRegularToolkitCache(final String cacheManagerName,
                                                                                    final String cacheName,
                                                                                    final CacheConfiguration ehcacheConfig) {
    final Configuration toolkitCacheConfig = createClusteredCacheConfig(ehcacheConfig, cacheManagerName);
    final String fullyQualifiedCacheName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName);
    addNonStopConfigForCache(ehcacheConfig, fullyQualifiedCacheName);
    ToolkitCacheInternal<String, Serializable> toolkitCache = getOrCreateToolkitCache(fullyQualifiedCacheName, toolkitCacheConfig);
    addCacheEntityInfo(cacheName, ehcacheConfig, fullyQualifiedCacheName);
    return toolkitCache;
  }

  private ToolkitCacheInternal<String, Serializable> getOrCreateToolkitCache(final String fullyQualifiedCacheName,
                                                                             final Configuration toolkitCacheConfig) {
    return (ToolkitCacheInternal<String, Serializable>) toolkit.getCache(fullyQualifiedCacheName, toolkitCacheConfig,
                                                                         Serializable.class);
  }

  @Override
  public ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(Ehcache cache) {
    return getOrCreateConfigChangeNotifier(cache.getCacheManager().getName(), cache.getName());
  }

  private ToolkitNotifier<CacheConfigChangeNotificationMsg> getOrCreateConfigChangeNotifier(String cacheManagerName, String cacheName) {
    String notifierName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                          + CONFIG_NOTIFIER_SUFFIX;
    ToolkitNotifier<CacheConfigChangeNotificationMsg> notifier = toolkit
        .getNotifier(notifierName, CacheConfigChangeNotificationMsg.class);
    addCacheMetaInfo(cacheName, ToolkitObjectType.NOTIFIER, notifierName);
    return notifier;
  }

  @Override
  public ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(Ehcache cache) {
    return getOrCreateCacheEventNotifier(cache.getCacheManager().getName(), cache.getName());
  }

  @Override
  public ToolkitNotifier<CacheDisposalNotification> getOrCreateCacheDisposalNotifier(Ehcache cache) {
    return toolkit.getNotifier(EhcacheEntitiesNaming.getToolkitCacheNameFor(cache.getCacheManager().getName(), cache.getName())
                               + DELIMITER + DISPOSAL_NOTIFIER_SUFFIX, CacheDisposalNotification.class);
  }

  private ToolkitNotifier<CacheEventNotificationMsg> getOrCreateCacheEventNotifier(String cacheManagerName, String cacheName) {
    String notifierName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                          + EVENT_NOTIFIER_SUFFIX;
    ToolkitNotifier<CacheEventNotificationMsg> notifier = toolkit.getNotifier(notifierName,
                                                                              CacheEventNotificationMsg.class);
    addCacheMetaInfo(cacheName, ToolkitObjectType.NOTIFIER, notifierName);
    return notifier;
  }

  private static Configuration createClusteredCacheConfig(final CacheConfiguration ehcacheConfig,
                                                          final String cacheManagerName) {
    ToolkitCacheConfigBuilder builder = new ToolkitCacheConfigBuilder();
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    builder.maxTTISeconds((int) ehcacheConfig.getTimeToIdleSeconds());
    builder.maxTTLSeconds((int) ehcacheConfig.getTimeToLiveSeconds());
    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());

    // Fix for Dev-9223. Dont set anything incase of Default value. Assuming tookit and ehcache defaults are aligned.
    if (ehcacheConfig.getMaxEntriesInCache() != CacheConfiguration.DEFAULT_MAX_ENTRIES_IN_CACHE) {
      if (ehcacheConfig.getMaxEntriesInCache() > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Values greater than Integer.MAX_VALUE are not currently supported.");
      } else {
        builder.maxTotalCount((int) ehcacheConfig.getMaxEntriesInCache());
      }
    }

    if (terracottaConfiguration.isSynchronousWrites()) {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.SYNCHRONOUS_STRONG);
    } else if (terracottaConfiguration.getConsistency() == Consistency.EVENTUAL) {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.EVENTUAL);
    } else {
      builder.consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.STRONG);
    }

    if (terracottaConfiguration.getConcurrency() == TerracottaConfiguration.DEFAULT_CONCURRENCY) {
      builder.concurrency(calculateCorrectConcurrency(ehcacheConfig));
    } else {
      builder.concurrency(terracottaConfiguration.getConcurrency());
    }

    builder.localCacheEnabled(terracottaConfiguration.isLocalCacheEnabled());
    builder.configField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME, cacheManagerName);
    builder.pinnedInLocalMemory(isPinnedInLocalMemory(ehcacheConfig));
    builder.evictionEnabled(!isPinnedInCache(ehcacheConfig));
    builder.maxCountLocalHeap((int) ehcacheConfig.getMaxEntriesLocalHeap());
    builder.maxBytesLocalHeap(ehcacheConfig.getMaxBytesLocalHeap());
    builder.maxBytesLocalOffheap(ehcacheConfig.getMaxBytesLocalOffHeap());
    builder.offheapEnabled(ehcacheConfig.isOverflowToOffHeap());
    builder.compressionEnabled(terracottaConfiguration.isCompressionEnabled());
    builder.copyOnReadEnabled(ehcacheConfig.isCopyOnRead());

    return builder.build();
  }

  private static boolean isPinnedInCache(final CacheConfiguration ehcacheConfig) {
    return ehcacheConfig.getPinningConfiguration() != null
           && ehcacheConfig.getPinningConfiguration().getStore() == PinningConfiguration.Store.INCACHE;
  }

  private static int calculateCorrectConcurrency(CacheConfiguration cacheConfiguration) {
    int maxElementOnDisk = cacheConfiguration.getMaxElementsOnDisk();
    if (maxElementOnDisk <= 0 || maxElementOnDisk >= ToolkitConfigFields.DEFAULT_CONCURRENCY) { return ToolkitConfigFields.DEFAULT_CONCURRENCY; }
    int concurrency = 1;
    while (concurrency * 2 <= maxElementOnDisk) {// this while loop is not very time consuming, maximum it will do 8
      // iterations
      concurrency *= 2;
    }
    return concurrency;
  }

  private static boolean isPinnedInLocalMemory(CacheConfiguration ehcacheConfig) {
    return ehcacheConfig.getPinningConfiguration() != null
           && ehcacheConfig.getPinningConfiguration().getStore() == PinningConfiguration.Store.LOCALMEMORY;
  }

  @Override
  public String getFullyQualifiedCacheName(Ehcache cache) {
    return EhcacheEntitiesNaming.getToolkitCacheNameFor(getCacheManagerName(cache), cache.getName());
  }

  private static String getCacheManagerName(Ehcache cache) {
    final String cacheMgrName;
    if (cache.getCacheManager().isNamed()) {
      cacheMgrName = cache.getCacheManager().getName();
    } else {
      cacheMgrName = TerracottaClusteredInstanceFactory.DEFAULT_CACHE_MANAGER_NAME;
    }
    return cacheMgrName;
  }

  @Override
  public ToolkitLock getOrCreateStoreLock(Ehcache cache) {
    return toolkit.getLock(getFullyQualifiedCacheName(cache) + DELIMITER + "storeRWLock");
  }

  @Override
  public ToolkitMap<String, AttributeExtractor> getOrCreateExtractorsMap(final String cacheManagerName, String cacheName) {
    // implemented in ee version
    throw new UnsupportedOperationException();
  }

  @Override
  public ToolkitMap<String, String> getOrCreateAttributeMap(final String cacheManagerName, String cacheName) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void shutdown() {
    if (clusteredCacheManagerEntity != null) {
      try {
        clusteredCacheManagerEntity.releaseUse();
      } catch (Exception e) {
        // Ignore - will be shutting down toolkit anyway
        LOGGER.debug("Exception occurred while releasing clustered cache manager entity use", e);
      }
    }
    clusteredEntityManager.dispose();
    toolkit.shutdown();
  }

  @Override
  public SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock> getOrCreateAllSoftLockMap(String cacheManagerName,
                                                                                                                            String cacheName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = new ToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.STRONG).build();
    String softLockCacheName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                               + ALL_SOFT_LOCKS_MAP_SUFFIX;
    ToolkitCache<String, SerializedReadCommittedClusteredSoftLock> map = toolkit
        .getCache(softLockCacheName, config, SerializedReadCommittedClusteredSoftLock.class);
    addCacheMetaInfo(cacheName, ToolkitObjectType.CACHE, softLockCacheName);
    return new SerializedToolkitCache<ClusteredSoftLockIDKey, SerializedReadCommittedClusteredSoftLock>(map);

  }

  @Override
  public ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> getOrCreateNewSoftLocksSet(String cacheManagerName,
                                                                                                  String cacheName) {
    String softLockMapName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                             + NEW_SOFT_LOCKS_LIST_SUFFIX;
    ToolkitMap<SerializedReadCommittedClusteredSoftLock, Integer> softLockMap = toolkit
        .getMap(softLockMapName, SerializedReadCommittedClusteredSoftLock.class, Integer.class);
    addCacheMetaInfo(cacheName, ToolkitObjectType.MAP, softLockMapName);
    return softLockMap;
  }

  @Override
  public ToolkitMap<String, AsyncConfig> getOrCreateAsyncConfigMap() {
    return toolkit.getMap(EhcacheEntitiesNaming.getAsyncConfigMapName(), String.class, AsyncConfig.class);
  }

  @Override
  public ToolkitMap<String, Set<String>> getOrCreateAsyncListNamesMap(String fullAsyncName, String cacheName) {
    ToolkitMap asyncListNames = toolkit.getMap(fullAsyncName, String.class, Set.class);
    addCacheMetaInfo(cacheName, ToolkitObjectType.MAP, fullAsyncName);
    addKeyRemoveInfo(cacheName, EhcacheEntitiesNaming.getAsyncConfigMapName(), fullAsyncName);
    return asyncListNames;
  }

  @Override
  public ToolkitListInternal getAsyncProcessingBucket(String bucketName, String cacheName) {
    ToolkitListInternal toolkitList = (ToolkitListInternal) toolkit.getList(bucketName, null);
    addCacheMetaInfo(cacheName, ToolkitObjectType.LIST, bucketName);
    return toolkitList;
  }

  @Override
  public ToolkitMap<String, Serializable> getOrCreateClusteredStoreConfigMap(String cacheManagerName, String cacheName) {
    String configMapName = EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName);
    ToolkitMap<String, Serializable> configMap = getOrCreateConfigMap(configMapName);
    addCacheMetaInfo(cacheName, ToolkitObjectType.MAP, configMapName);
    return configMap;
  }

  private ToolkitMap<String, Serializable> getOrCreateConfigMap(final String fullyQualifiedCacheName) {
    // TODO: what should be the local cache config for the map?
    return toolkit.getMap(EhcacheEntitiesNaming.getToolkitCacheConfigMapName(fullyQualifiedCacheName), String.class, Serializable.class);
  }

  @Override
  public SerializedToolkitCache<TransactionID, Decision> getOrCreateTransactionCommitStateMap(String cacheManagerName) {
    // TODO: what should be the local cache config for the map?
    Configuration config = new ToolkitStoreConfigBuilder()
        .consistency(org.terracotta.toolkit.store.ToolkitConfigFields.Consistency.SYNCHRONOUS_STRONG).build();
    ToolkitCache<String, Decision> map = toolkit.getCache(cacheManagerName + DELIMITER
                                                              + EHCACHE_TXNS_DECISION_STATE_MAP_NAME, config,
                                                          Decision.class);
    return new SerializedToolkitCache<TransactionID, Decision>(map);
  }

  @Override
  public ToolkitLock getSoftLockWriteLock(String cacheManagerName, String cacheName, TransactionID transactionID,
                                          Object key) {

    return toolkit.getLock(EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                           + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                           + EHCACHE_TXNS_SOFTLOCK_WRITE_LOCK_NAME);
  }

  @Override
  public ToolkitLock getLockForCache(Ehcache cache, String lockName) {
    return toolkit.getLock(getFullyQualifiedCacheName(cache) + DELIMITER + lockName);
  }



  private static String serializeToString(Object serializable) {
    try {
      return SerializationHelper.serializeToString(serializable);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ToolkitReadWriteLock getSoftLockFreezeLock(String cacheManagerName, String cacheName,
                                                    TransactionID transactionID, Object key) {

    return toolkit.getReadWriteLock(EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                                    + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                                    + EHCACHE_TXNS_SOFTLOCK_FREEZE_LOCK_NAME);
  }

  @Override
  public ToolkitReadWriteLock getSoftLockNotifierLock(String cacheManagerName, String cacheName,
                                                      TransactionID transactionID, Object key) {

    return toolkit.getReadWriteLock(EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName) + DELIMITER
                                    + serializeToString(transactionID) + DELIMITER + serializeToString(key) + DELIMITER
                                    + EHCACHE_TXNS_SOFTLOCK_NOTIFIER_LOCK_NAME);
  }

  @Override
  public boolean destroy(final String cacheManagerName, final String cacheName) {
    getOrCreateAllSoftLockMap(cacheManagerName, cacheName).destroy();
    getOrCreateNewSoftLocksSet(cacheManagerName, cacheName).destroy();
    getOrCreateCacheEventNotifier(cacheManagerName, cacheName).destroy();
    getOrCreateConfigChangeNotifier(cacheManagerName, cacheName).destroy();
    getOrCreateToolkitCache(EhcacheEntitiesNaming.getToolkitCacheNameFor(cacheManagerName, cacheName),
                            new ToolkitCacheConfigBuilder().maxCountLocalHeap(1).maxBytesLocalOffheap(0).build())
        .destroy();

    // We always write the transactional mode into this config map, so theoretically if the cache existed, this map
    // won't be empty.
    ToolkitMap clusteredStoreConfigMap = getOrCreateClusteredStoreConfigMap(cacheManagerName, cacheName);
    boolean existed = !clusteredStoreConfigMap.isEmpty();
    clusteredStoreConfigMap.destroy();

    return existed;
  }

  protected void addNonStopConfigForCache(final CacheConfiguration ehcacheConfig, final String fullyQualifiedCacheName) {
    final TerracottaConfiguration terracottaConfiguration = ehcacheConfig.getTerracottaConfiguration();
    ToolkitNonStopConfiguration nonstopConfiguration = new ToolkitNonStopConfiguration(
                                                                                       terracottaConfiguration
                                                                                           .getNonstopConfiguration());
    toolkit.getFeature(ToolkitFeatureType.NONSTOP).getNonStopConfigurationRegistry()
        .registerForInstance(nonstopConfiguration, fullyQualifiedCacheName, ToolkitObjectType.CACHE);
  }

  @Override
  public void removeNonStopConfigforCache(Ehcache cache) {
    toolkit.getFeature(ToolkitFeatureType.NONSTOP).getNonStopConfigurationRegistry()
        .deregisterForInstance(getFullyQualifiedCacheName(cache), ToolkitObjectType.CACHE);

  }

  protected void addCacheMetaInfo(String cacheName, ToolkitObjectType type, String dsName) {
    ToolkitBackedClusteredCacheManager tbccm = (ToolkitBackedClusteredCacheManager) clusteredCacheManagerEntity;
    tbccm.addCacheMetaInfo(cacheName, type, dsName);
  }

  private void addKeyRemoveInfo(String cacheName, String toolkitMapName, String keytoBeRemoved) {
    ToolkitBackedClusteredCacheManager tbccm = (ToolkitBackedClusteredCacheManager) clusteredCacheManagerEntity;
    tbccm.addKeyRemoveInfo(cacheName, toolkitMapName, keytoBeRemoved);
  }

  @Override
  public void linkClusteredCacheManager(String cacheManagerName, net.sf.ehcache.config.Configuration configuration) {
    if (clusteredCacheManagerEntity == null) {
      logCacheManagerConfigInTerracottaClientLogs(cacheManagerName, configuration);

      ClusteredCacheManager clusteredCacheManager = clusteredEntityManager.getRootEntity(cacheManagerName,
          ClusteredCacheManager.class);
      ToolkitReadWriteLock cmRWLock = clusteredEntityManager.getEntityLock(EhcacheEntitiesNaming
          .getCacheManagerLockNameFor(cacheManagerName));
      ToolkitLock cmWriteLock = cmRWLock.writeLock();
      while (clusteredCacheManager == null) {
        if (cmWriteLock.tryLock()) {
          try {
            clusteredCacheManager = createClusteredCacheManagerEntity(cacheManagerName, configuration);
          } finally {
            cmWriteLock.unlock();
          }
        } else {
          clusteredCacheManager = clusteredEntityManager.getRootEntity(cacheManagerName, ClusteredCacheManager.class);
        }
      }
      clusteredCacheManagerEntity = clusteredCacheManager;
      entityNames.setCacheManagerName(cacheManagerName);
    }
  }

  private void logCacheManagerConfigInTerracottaClientLogs(String cacheManagerName, net.sf.ehcache.config.Configuration configuration) {
    ToolkitLogger logger = ((ToolkitInternal)toolkit).getLogger(CONFIG_LOGGER_NAME);
    if (logger.isInfoEnabled()) {
      try {
        logger.info("Configuration for clustered cache manager " + cacheManagerName + ":\n" +
                    convertConfigurationToXMLString(configuration, cacheManagerName, true));
      } catch (Exception e) {
        logger.warn("Exception while trying to log configuration for clustered cache manager " + cacheManagerName, e);
      }
    }
  }

  private ClusteredCacheManager createClusteredCacheManagerEntity(String cacheManagerName, net.sf.ehcache.config.Configuration configuration) {
    ClusteredCacheManager clusteredCacheManager;
    String xmlConfig = convertConfigurationToXMLString(configuration, cacheManagerName, true);
    clusteredCacheManager = new ToolkitBackedClusteredCacheManager(cacheManagerName,
                                                                   new ClusteredCacheManagerConfiguration(xmlConfig));
    ClusteredCacheManager existing = clusteredEntityManager.addRootEntityIfAbsent(cacheManagerName, ClusteredCacheManager.class, clusteredCacheManager);
    if (existing != null) {
      clusteredCacheManager = existing;
    }
    return clusteredCacheManager;
  }

  @Override
  public ToolkitMap<String, Serializable> getOrCreateCacheManagerMetaInfoMap(String cacheManagerName) {
    String configMapName = EhcacheEntitiesNaming.getCacheManagerConfigMapName(cacheManagerName);
    ToolkitMap<String, Serializable> configMap = toolkit.getMap(configMapName, String.class, Serializable.class);
    return configMap;
  }

  void addCacheEntityInfo(final String cacheName, final CacheConfiguration ehcacheConfig, String toolkitCacheName) {
    if (clusteredCacheManagerEntity == null) {
      throw new IllegalStateException(format("ClusteredCacheManger entity not configured for cache %s", cacheName));
    }

    logCacheConfigInTerracottaClientLogs(cacheName, ehcacheConfig);

    ClusteredCache cacheEntity = clusteredCacheManagerEntity.getCache(cacheName);
    if (cacheEntity == null) {
      ToolkitReadWriteLock cacheRWLock = clusteredCacheManagerEntity.getCacheLock(cacheName);
      ToolkitLock cacheWriteLock = cacheRWLock.writeLock();
      while (cacheEntity == null) {
        if (cacheWriteLock.tryLock()) {
          try {
            cacheEntity = createClusteredCacheEntity(cacheName, ehcacheConfig, toolkitCacheName);
          } finally {
            cacheWriteLock.unlock();
          }
        } else {
          cacheEntity = clusteredCacheManagerEntity.getCache(cacheName);
        }
      }
    }
    // TODO check some config elements
    clusteredCacheManagerEntity.markCacheInUse(cacheEntity);
    entityNames.addCacheName(cacheName);
  }

  private void logCacheConfigInTerracottaClientLogs(String cacheName, CacheConfiguration ehcacheConfig) {
    ToolkitLogger logger = ((ToolkitInternal)toolkit).getLogger(CONFIG_LOGGER_NAME);
    if (logger.isInfoEnabled()) {
      try {
        logger.info("Client configuration for clustered cache named " + cacheName + ":\n(clustered properties may differ in runtime cache depending on configuration used at creation time)\n" +
                    convertCacheConfigurationToXMLString(ehcacheConfig));
      } catch (Exception e) {
        logger.warn("Exception while trying to log configuration for clustered cache " + cacheName, e);
      }
    }
  }

  private ClusteredCache createClusteredCacheEntity(String cacheName, CacheConfiguration ehcacheConfig, String toolkitCacheName) {
    ClusteredCacheConfiguration clusteredConfiguration = createClusteredCacheConfiguration(ehcacheConfig);
    ClusteredCache cacheEntity = new ToolkitBackedClusteredCache(cacheName, clusteredConfiguration, toolkitCacheName);

    ClusteredCache existing = clusteredCacheManagerEntity.addCacheIfAbsent(cacheName, cacheEntity);
    if (existing != null) {
      cacheEntity = existing;
    }
    return cacheEntity;
  }

  private ClusteredCacheConfiguration createClusteredCacheConfiguration(CacheConfiguration ehcacheConfig) {
    String xmlConfig = convertCacheConfigurationToXMLString(ehcacheConfig);
    return new ClusteredCacheConfiguration(xmlConfig);
  }

  private String convertCacheConfigurationToXMLString(CacheConfiguration ehcacheConfig) {
    net.sf.ehcache.config.Configuration configuration = parseCacheManagerConfiguration(clusteredCacheManagerEntity.getConfiguration()
        .getConfigurationAsText());
    return ConfigurationUtil.generateCacheConfigurationText(configuration, ehcacheConfig);
  }

  @Override
  public void unlinkCache(String cacheName) {
    entityNames.removeCacheName(cacheName);
    ClusteredCache cacheEntity = clusteredCacheManagerEntity.getCache(cacheName);
    clusteredCacheManagerEntity.releaseCacheUse(cacheEntity);
  }

  @Override
  public void clusterRejoined() {
    String cacheManagerName = entityNames.cacheManagerName;
    synchronized (entityNames) {
      ClusteredCacheManager clusteredCacheManager = clusteredEntityManager.getRootEntity(cacheManagerName,
                                                                                         ClusteredCacheManager.class);
      if (clusteredCacheManager == null) {
        LOGGER.error("Cache Manager {} has been destroyed by some other node - shutting down to prevent any further use of clustered features", cacheManagerName);
        shutdown();
      } else {
        // release Cache Manager lock after rejoin
        try {
          clusteredCacheManager.releaseUse();
        } catch (Exception e) {
          // Ignore - just trying to clean up
          LOGGER.trace("Exception trying to release cache manager {} after rejoin", entityNames.cacheManagerName);
        }

        // release cache read lock after rejoin
        for (String cacheName : entityNames.getCacheNames()) {
          ClusteredCache cacheEntity = clusteredCacheManagerEntity.getCache(cacheName);
          if (cacheEntity != null) {
            try {
              clusteredCacheManagerEntity.releaseCacheUse(cacheEntity);
            } catch (Exception e) {
              // Ignore - just trying to clean up
              LOGGER.trace("Exception trying to release cache {} after rejoin", cacheName);
            }
          }
        }

        int retryCount = 0;
        boolean success = false;
        while (retryCount < RETRY_MARK_IN_USE_AFTER_REJOIN) {
          // grab Cache Manager read lock after rejoin
          try {
            clusteredCacheManager.markInUse();
            success = (clusteredEntityManager.getRootEntity(cacheManagerName, ClusteredCacheManager.class) != null);
            break;
          } catch (Exception e) {
            try {
              TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e1) {
              // Do nothing
            }
            retryCount++;
          }
        }
        if (!success) {
          LOGGER.error("Unable to mark cache manager {} in use - shutting down to prevent any further use of clustered features", cacheManagerName);
          shutdown();
        } else {
          // grab cache read lock after rejoin
          for (String cacheName : entityNames.getCacheNames()) {
            boolean successCache = false;
            int retryCountCache = 0;
            while (!successCache && retryCountCache < RETRY_MARK_IN_USE_AFTER_REJOIN) {
              ClusteredCache cacheEntity = clusteredCacheManagerEntity.getCache(cacheName);
              if (cacheEntity == null) {
                LOGGER.error("Cache " + cacheName + " has been destroyed by some other node");
                successCache = true;
              } else {
                try {
                  clusteredCacheManagerEntity.markCacheInUse(cacheEntity);
                  successCache = true;
                } catch (Exception e) {
                  try {
                    TimeUnit.SECONDS.sleep(1);
                  } catch (InterruptedException e1) {
                    // Do nothing
                  }
                  retryCountCache++;
                }
              }
            }
          }
        }
      }
    }
  }

  private String convertConfigurationToXMLString(net.sf.ehcache.config.Configuration configuration, String cacheManagerName, boolean stripCacheConfigs) {
    net.sf.ehcache.config.Configuration targetConfiguration = cloneConfiguration(configuration);
    targetConfiguration.setName(cacheManagerName);
    if (stripCacheConfigs) {
      targetConfiguration.getCacheConfigurations().clear();
    }
    return ConfigurationUtil.generateCacheManagerConfigurationText(targetConfiguration);
  }

  private net.sf.ehcache.config.Configuration cloneConfiguration(net.sf.ehcache.config.Configuration configuration) {
    String tmp = ConfigurationUtil.generateCacheManagerConfigurationText(configuration);
    net.sf.ehcache.config.Configuration targetConfiguration;
    targetConfiguration = parseCacheManagerConfiguration(tmp);
    return targetConfiguration;
  }

  private net.sf.ehcache.config.Configuration parseCacheManagerConfiguration(String xmlCacheManagerConfig) {
    net.sf.ehcache.config.Configuration targetConfiguration;
    targetConfiguration = ConfigurationFactory.parseConfiguration(new BufferedInputStream(new ByteArrayInputStream(xmlCacheManagerConfig
        .getBytes())));
    return targetConfiguration;
  }

  private class EntityNamesHolder {
    private String            cacheManagerName;
    private final Set<String> cacheNames;

    private EntityNamesHolder() {
      cacheNames = new HashSet<String>();
    }

    private synchronized void setCacheManagerName(String cacheMgrName) {
      if (cacheManagerName == null) {
        cacheManagerName = cacheMgrName;
        ToolkitInstanceFactoryImpl.this.clusteredCacheManagerEntity.markInUse();
      }
    }

    private synchronized void addCacheName(String cacheName) {
      cacheNames.add(cacheName);
    }

    private synchronized void removeCacheName(String cacheName) {
      cacheNames.remove(cacheName);
    }

    private Set<String> getCacheNames() {
      return Collections.unmodifiableSet(cacheNames);
    }
  }

  private static class EhcacheTcConfig {
    private enum Type {
      URL, EMBEDDED_TC_CONFIG, FILE
    }

    private final Type   type;
    private final String tcConfigUrlOrSnippet;

    private EhcacheTcConfig(Type type, String config) {
      this.type = type;
      this.tcConfigUrlOrSnippet = config;
    }

    public static EhcacheTcConfig create(TerracottaClientConfiguration config) {
      if (config.isUrlConfig()) {
        String urlOrFilePath = config.getUrl();
        if (isFile(urlOrFilePath)) {
          return new EhcacheTcConfig(Type.FILE, slurpFile(urlOrFilePath));
        } else if (isValidURL(urlOrFilePath)) {
          return new EhcacheTcConfig(Type.EMBEDDED_TC_CONFIG, fetchConfigFromURL(urlOrFilePath));
        } else {
          return new EhcacheTcConfig(Type.URL, urlOrFilePath);
        }
      } else {
        return new EhcacheTcConfig(Type.EMBEDDED_TC_CONFIG, config.getEmbeddedConfig());
      }
    }

    private static String slurpFile(String urlOrFilePath) {
      try {
        return fetchConfigFromStream(new FileInputStream(urlOrFilePath));
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }

    private static boolean isFile(String urlOrFilePath) {
      File file = new File(urlOrFilePath);
      return file.exists() && file.isFile();
    }

    private static String fetchConfigFromURL(String urlOrFilePath) {
      try {
        return fetchConfigFromStream(new URL(urlOrFilePath).openStream());
      } catch (MalformedURLException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static String fetchConfigFromStream(InputStream inputStream) {
      try {
        StringBuilder builder = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        String line = null;
        while ((line = br.readLine()) != null) {
          builder.append(line);
        }
        return builder.toString();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    private static boolean isValidURL(String urlOrFilePath) {
      try {
        new URL(urlOrFilePath);
        return true;
      } catch (MalformedURLException e) {
        return false;
      }
    }

  }
}
