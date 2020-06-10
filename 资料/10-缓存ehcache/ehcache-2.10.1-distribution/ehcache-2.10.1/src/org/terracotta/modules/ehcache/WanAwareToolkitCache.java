package org.terracotta.modules.ehcache;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.NonstopConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.modules.ehcache.wan.Watchable;
import org.terracotta.toolkit.cache.ToolkitCacheListener;
import org.terracotta.toolkit.cluster.ClusterNode;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.feature.NonStopFeature;
import org.terracotta.toolkit.internal.cache.BufferingToolkitCache;
import org.terracotta.toolkit.internal.cache.ToolkitCacheInternal;
import org.terracotta.toolkit.internal.cache.ToolkitValueComparator;
import org.terracotta.toolkit.internal.cache.VersionUpdateListener;
import org.terracotta.toolkit.internal.cache.VersionedValue;
import org.terracotta.toolkit.nonstop.NonStopException;
import org.terracotta.toolkit.search.QueryBuilder;
import org.terracotta.toolkit.search.attribute.ToolkitAttributeExtractor;
import org.terracotta.toolkit.store.ToolkitConfigFields;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A wrapper around {@link ToolkitCacheInternal}
 * which rejects all user actions if the WAN-enabled cache is deactivated.
 *
 * @author Eugene Shelestovich
 */
public class WanAwareToolkitCache<K, V> implements BufferingToolkitCache<K, V>, Watchable {

  private static final Logger LOGGER = LoggerFactory.getLogger(WanAwareToolkitCache.class);
  private static final String CACHE_ACTIVE_KEY = "WAN-CACHE-ACTIVE";
  private static final String ORCHESTRATOR_ALIVE_KEY = "ORCHESTRATOR-ALIVE";
  private static final String ORCHESTRATOR_MODE = "ORCHESTRATOR-MODE";
  private static final String REPLICATION_MODE = "REPLICATION-MODE";

  private final BufferingToolkitCache<K, V> delegate;
  private final ConcurrentMap<String, Serializable> configMap;
  private final NonStopFeature nonStop;
  private final ToolkitLock configMapLock;
  private final ToolkitLock activeLock;
  private final CacheConfiguration cacheConfiguration;
  private final boolean masterCache;
  private final boolean bidirectional;

  public WanAwareToolkitCache(final BufferingToolkitCache<K, V> delegate,
                              final ToolkitMap<String, Serializable> configMap,
                              final NonStopFeature nonStop,
                              final ToolkitLock activeLock,
                              final CacheConfiguration cacheConfiguration,
                              final boolean masterCache,
                              final boolean bidirectional) {
    this(delegate, configMap, nonStop, configMap.getReadWriteLock().writeLock(), activeLock, cacheConfiguration, masterCache, bidirectional);
  }

  /**
   * Constructor for Unit Tests only
   */
  WanAwareToolkitCache(final BufferingToolkitCache<K, V> delegate,
                       final ConcurrentMap<String, Serializable> configMap,
                       final NonStopFeature nonStop,
                       final ToolkitLock configMapLock,
                       final ToolkitLock activeLock,
                       final CacheConfiguration cacheConfiguration,
                       final boolean masterCache,
                       final boolean bidirectional) {
    this.delegate = delegate;
    this.configMap = configMap;
    this.nonStop = nonStop;
    this.configMapLock = configMapLock;
    this.activeLock = activeLock;
    this.cacheConfiguration = cacheConfiguration;
    this.masterCache = masterCache;
    this.bidirectional = bidirectional;
    configMap.putIfAbsent(CACHE_ACTIVE_KEY, false);
    configMap.putIfAbsent(ORCHESTRATOR_ALIVE_KEY, false);
    configMap.putIfAbsent(ORCHESTRATOR_MODE, masterCache ? "Master" : "Replica");
    configMap.putIfAbsent(REPLICATION_MODE, bidirectional ? "BIDIRECTIONAL" : "UNIDIRECTIONAL");
  }

  /**
   * Can the cache handle user actions ?
   *
   * @return {@code true} if the cache is active, {@code false} otherwise
   */
  public boolean isReady() {
    Boolean active = (Boolean) configMap.get(CACHE_ACTIVE_KEY);
    if (isMasterCache()) {
      return active != null && active;
    }

    return active != null && active && (isOrchestratorAlive() || !bidirectional);
  }

  /**
   * Activates WAN-enabled cache, so it can start handling user actions.
   *
   * @return {@code true} if the cache's state was updated, otherwise {@code false}
   */
  public boolean activate() {
    boolean updated = setState(true);
    notifyClients();
    return updated;
  }

  /**
   * Deactivates WAN-enabled cache, so it rejects all user actions.
   *
   * @return {@code true} if the cache's state was updated, otherwise {@code false}
   */
  public boolean deactivate() {
    return setState(false);
  }

  private boolean setState(boolean active) {
    return configMap.replace(CACHE_ACTIVE_KEY, !active, active);
  }

  @Override
  public Map<Object, Set<ClusterNode>> getNodesWithKeys(final Set portableKeys) {
    waitIfRequired();
    return delegate.getNodesWithKeys(portableKeys);
  }

  @Override
  public void unlockedPutNoReturn(final K k, final V v, final int createTime, final int customTTI, final int customTTL) {
    waitIfRequired();
    delegate.unlockedPutNoReturn(k, v, createTime, customTTI, customTTL);
  }

  @Override
  public void unlockedRemoveNoReturn(final Object k) {
    waitIfRequired();
    delegate.unlockedRemoveNoReturn(k);
  }

  @Override
  public V unlockedGet(final Object k, final boolean quiet) {
    waitIfRequired();
    return delegate.unlockedGet(k, quiet);
  }

  @Override
  public Map<K, V> unlockedGetAll(final Collection<K> keys, final boolean quiet) {
    waitIfRequired();
    return delegate.unlockedGetAll(keys, quiet);
  }

  @Override
  public void removeAll(final Set<K> keys) {
    waitIfRequired();
    delegate.removeAll(keys);
  }


  @Override
  public V put(final K key, final V value, final int createTimeInSecs, final int customMaxTTISeconds,
               final int customMaxTTLSeconds) {
    waitIfRequired();
    return delegate.put(key, value, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public V putIfAbsent(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                       final int maxTTLSeconds) {
    waitIfRequired();
    return delegate.putIfAbsent(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public void putNoReturn(final K key, final V value, final long createTimeInSecs, final int maxTTISeconds,
                          final int maxTTLSeconds) {
    waitIfRequired();
    delegate.putNoReturn(key, value, createTimeInSecs, maxTTISeconds, maxTTLSeconds);
  }

  @Override
  public Map<K, V> getAllQuiet(final Collection<K> keys) {
    waitIfRequired();
    return delegate.getAllQuiet(keys);
  }

  @Override
  public V getQuiet(final Object key) {
    waitIfRequired();
    return delegate.getQuiet(key);
  }


  @Override
  public Map<K, V> getAll(final Collection<? extends K> keys) {
    waitIfRequired();
    return delegate.getAll(keys);
  }

  @Override
  public void putNoReturn(final K key, final V value) {
    waitIfRequired();
    delegate.putNoReturn(key, value);
  }

  @Override
  public void removeNoReturn(final Object key) {
    waitIfRequired();
    delegate.removeNoReturn(key);
  }

  @Override
  public V putIfAbsent(final K key, final V value) {
    waitIfRequired();
    return delegate.putIfAbsent(key, value);
  }

  @Override
  public boolean remove(final Object key, final Object value) {
    waitIfRequired();
    return delegate.remove(key, value);
  }

  @Override
  public boolean replace(final K key, final V oldValue, final V newValue) {
    waitIfRequired();
    return delegate.replace(key, oldValue, newValue);
  }

  @Override
  public V replace(final K key, final V value) {
    waitIfRequired();
    return delegate.replace(key, value);
  }

  @Override
  public int size() {
    waitIfRequired();
    return delegate.size();
  }

  @Override
  public int quickSize() {
    waitIfRequired();
    return delegate.quickSize();
  }

  @Override
  public boolean isEmpty() {
    waitIfRequired();
    return delegate.isEmpty();
  }

  @Override
  public boolean containsKey(final Object key) {
    waitIfRequired();
    return delegate.containsKey(key);
  }

  @Override
  public boolean containsValue(final Object value) {
    waitIfRequired();
    return delegate.containsValue(value);
  }

  @Override
  public V get(final Object key) {
    waitIfRequired();
    return delegate.get(key);
  }

  @Override
  public V put(final K key, final V value) {
    waitIfRequired();
    return delegate.put(key, value);
  }

  @Override
  public V remove(final Object key) {
    waitIfRequired();
    return delegate.remove(key);
  }

  @Override
  public void putAll(final Map<? extends K, ? extends V> m) {
    waitIfRequired();
    delegate.putAll(m);
  }

  @Override
  public void clear() {
    waitIfRequired();
    delegate.clear();
  }

  @Override
  public void quickClear() {
    waitIfRequired();
    delegate.quickClear();
  }

  @Override
  public Set<K> keySet() {
    waitIfRequired();
    return delegate.keySet();
  }

  @Override
  public Collection<V> values() {
    waitIfRequired();
    return delegate.values();
  }

  @Override
  public Set<Entry<K, V>> entrySet() {
    waitIfRequired();
    return delegate.entrySet();
  }

  @Override
  public void destroy() {
    waitIfRequired();
    delegate.destroy();
  }

  @Override
  public boolean remove(Object key, Object value, ToolkitValueComparator<V> comparator) {
    waitIfRequired();
    return delegate.remove(key, value, comparator);
  }

  @Override
  public boolean replace(K key, V oldValue, V newValue, ToolkitValueComparator<V> comparator) {
    waitIfRequired();
    return delegate.replace(key, oldValue, newValue, comparator);
  }

  @Override
  public boolean equals(final Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public boolean isDestroyed() {
    return delegate.isDestroyed();
  }


  @Override
  public void setAttributeExtractor(final ToolkitAttributeExtractor<K, V> attrExtractor) {
    delegate.setAttributeExtractor(attrExtractor);
  }

  @Override
  public QueryBuilder createQueryBuilder() {
    return delegate.createQueryBuilder();
  }

  @Override
  public boolean isBulkLoadEnabled() {
    return delegate.isBulkLoadEnabled();
  }

  @Override
  public boolean isNodeBulkLoadEnabled() {
    return delegate.isNodeBulkLoadEnabled();
  }

  @Override
  public void setNodeBulkLoadEnabled(final boolean enabledBulkLoad) {
    delegate.setNodeBulkLoadEnabled(enabledBulkLoad);
  }

  @Override
  public void waitUntilBulkLoadComplete() throws InterruptedException {
    delegate.waitUntilBulkLoadComplete();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public void clearLocalCache() {
    delegate.clearLocalCache();
  }

  @Override
  public V unsafeLocalGet(final Object key) {
    return delegate.unsafeLocalGet(key);
  }

  @Override
  public boolean containsLocalKey(final Object key) {
    return delegate.containsLocalKey(key);
  }

  @Override
  public int localSize() {
    return delegate.localSize();
  }

  @Override
  public Set<K> localKeySet() {
    return delegate.localKeySet();
  }

  @Override
  public long localOnHeapSizeInBytes() {
    return delegate.localOnHeapSizeInBytes();
  }

  @Override
  public long localOffHeapSizeInBytes() {
    return delegate.localOffHeapSizeInBytes();
  }

  @Override
  public int localOnHeapSize() {
    return delegate.localOnHeapSize();
  }

  @Override
  public int localOffHeapSize() {
    return delegate.localOffHeapSize();
  }

  @Override
  public boolean containsKeyLocalOnHeap(final Object key) {
    return delegate.containsKeyLocalOnHeap(key);
  }

  @Override
  public boolean containsKeyLocalOffHeap(final Object key) {
    return delegate.containsKeyLocalOffHeap(key);
  }

  @Override
  public void disposeLocally() {
    delegate.disposeLocally();
  }

  @Override
  public ToolkitReadWriteLock createLockForKey(final K key) {
    return delegate.createLockForKey(key);
  }

  @Override
  public void setConfigField(final String name, final Serializable value) {
    delegate.setConfigField(name, value);
  }

  @Override
  public Configuration getConfiguration() {
    return delegate.getConfiguration();
  }

  @Override
  public void addListener(final ToolkitCacheListener<K> listener) {
    delegate.addListener(listener);
  }


  // **************** Methods called from Orchestrator - START **********************************
  // These methods should not wait for cache to become active
  // *********************************************************************************************

  @Override
  public void putIfAbsentVersioned(final K key, final V value, final long version) {
    delegate.putIfAbsentVersioned(key, value, version);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version) {
    delegate.putVersioned(key, value, version);
  }

  @Override
  public void putVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                           final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    delegate.putVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void putIfAbsentVersioned(final K key, final V value, final long version, final int createTimeInSecs,
                                   final int customMaxTTISeconds, final int customMaxTTLSeconds) {
    delegate.putIfAbsentVersioned(key, value, version, createTimeInSecs, customMaxTTISeconds, customMaxTTLSeconds);
  }

  @Override
  public void unlockedPutNoReturnVersioned(final K k, final V v, final long version, final int createTime,
                                           final int customTTI, final int customTTL) {
    delegate.unlockedPutNoReturnVersioned(k, v, version, createTime, customTTI, customTTL);
  }

  @Override
  public void removeVersioned(final Object key, final long version) {
    delegate.removeVersioned(key, version);
  }

  @Override
  public void registerVersionUpdateListener(final VersionUpdateListener listener) {
    delegate.registerVersionUpdateListener(listener);
  }

  @Override
  public void unregisterVersionUpdateListener(final VersionUpdateListener listener) {
    delegate.unregisterVersionUpdateListener(listener);
  }

  @Override
  public Set<K> keySetForSegment(final int segmentIndex) {
    return delegate.keySetForSegment(segmentIndex);
  }

  @Override
  public VersionedValue<V> getVersionedValue(final Object key) {
    return delegate.getVersionedValue(key);
  }

  @Override
  public Map<K, VersionedValue<V>> getAllVersioned(final Collection<K> keys) {
    return delegate.getAllVersioned(keys);
  }

  @Override
  public void removeListener(final ToolkitCacheListener<K> listener) {
    delegate.removeListener(listener);
  }

  @Override
  public void unlockedRemoveNoReturnVersioned(final Object key, final long version) {
    delegate.unlockedRemoveNoReturnVersioned(key, version);
  }

  @Override
  public void startBuffering() {
    delegate.startBuffering();
  }

  @Override
  public boolean isBuffering() {
    return delegate.isBuffering();
  }

  @Override
  public void stopBuffering() {
    delegate.stopBuffering();
  }

  @Override
  public void flushBuffer() {
    delegate.flushBuffer();
  }

  /**
   * Same as {@link #clear()}, except that it does not generate any server events and completely ignores
   * {@link #isReady()} flag.
   */
  @Override
  public void clearVersioned() {
    delegate.clearVersioned();
  }


  // **************** Methods called from Orchestrator - END **********************************
  // ********************************************************************************************


  /**
   * This method makes the current thread wait until the Cache becomes active or the NonStop timeout breaches.
   */
  private void waitIfRequired() {
    checkImmediateTimeout();
    if (!isReady()) {
      LOGGER.info("Cache '{}' not active. Waiting for the Orchestrator to mark it active", delegate.getName());
      waitUntilActive();
      LOGGER.info("Cache '{}' is now active", delegate.getName());
    }
  }

  void waitUntilActive() {
    boolean interrupted = false;
    configMapLock.lock();
    try {
      while (!isReady()) {
        checkImmediateTimeout();
        try {
          configMapLock.getCondition().await();
        } catch (InterruptedException e) {
          if (nonStop.isTimedOut()) {
            LOGGER.error("Operation timed-out while waiting for the cache '{}' to become active",
                delegate.getName());
            throw new NonStopException("Cache '" + delegate.getName() + "' not active currently.");
          } else {
            interrupted = true;
          }
        }
      }
    } finally {
      configMapLock.unlock();
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void checkImmediateTimeout() {
    if (isImmediateNonStopTimeout() && !isMasterCache() && !isOrchestratorAlive() && bidirectional) {
      throw new NonStopException("Orchestrator for cache '" + name() + "' is not alive.");
    }
  }

  private boolean isMasterCache() {
    return masterCache;
  }

  boolean isOrchestratorAlive() {
    Boolean orchestratorLive = (Boolean)configMap.get(ORCHESTRATOR_ALIVE_KEY);
    return orchestratorLive != null && orchestratorLive;
  }

  /**
   * Notifies the waiting client.
   */
  void notifyClients() {
    configMapLock.lock();
    try {
      configMapLock.getCondition().signalAll();
    } finally {
      configMapLock.unlock();
    }
  }

  /**
   * This method is called ony for replica caches by the Orchestrator
   */
  public void setUnlimitedCapacity() {
    LOGGER.info("Setting cache '{}' to be unlimited as it is a Replica.", delegate.getName());
    setConfigField(ToolkitConfigFields.MAX_TTL_SECONDS_FIELD_NAME, 0);
    setConfigField(ToolkitConfigFields.MAX_TTI_SECONDS_FIELD_NAME, 0);
  }

  @Override
  public void goLive() {
    final int sleepTime = 1 + (int) (Math.random() * 3);
    while (!activeLock.isHeldByCurrentThread()) {
      try {
        TimeUnit.SECONDS.sleep(sleepTime); // just a favorable condition for load distribution
        activeLock.lock();
        markOrchestratorAlive();
      } catch (Exception e) {
        LOGGER.error("Exception occurred while waiting for active lock for cache '{}'", getName(), e);
      }
    }
  }

  @Override
  public void die() {
    // no-op for the time being
  }

  boolean markOrchestratorDead() {
    if (configMap.replace(ORCHESTRATOR_ALIVE_KEY, true, false)) {
      notifyClients();
      if (bidirectional) {
        LOGGER.error("Orchestrator is not running for cache '{}'. Marking it as dead.", getName());
      } else {
        LOGGER.warn("Orchestrator is not running for cache '{}'. Cache remains operational, but it won't receive any subsequent updates over WAN.", getName());
      }
      return true;
    } else {
      return false;
    }
  }

  void markOrchestratorAlive() {
    configMap.put(ORCHESTRATOR_ALIVE_KEY, true);
    notifyClients();
  }

  @Override
  public boolean probeLiveness() {
    if (activeLock.tryLock()) {
      try {
        markOrchestratorDead();
        return false;
      } finally {
        activeLock.unlock();
      }
    }
    return true;
  }

  @Override
  public String name() {
    return getName();
  }

  private boolean isImmediateNonStopTimeout() {
    if (cacheConfiguration.getTerracottaConfiguration() == null) {
      return false;
    }
    NonstopConfiguration nonstopConfig = cacheConfiguration.getTerracottaConfiguration().getNonstopConfiguration();
    return nonstopConfig != null && nonstopConfig.isImmediateTimeout();
  }
}
