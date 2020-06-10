package com.terracotta.entity.ehcache;

import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.entity.RootEntity;

import java.util.Map;

/**
 * ClusteredCacheManager
 */
public interface ClusteredCacheManager extends RootEntity<ClusteredCacheManagerConfiguration> {
  Map<String, ClusteredCache> getCaches();

  ClusteredCache getCache(String cacheName);

  /**
   * Method for adding a {@link com.terracotta.entity.ehcache.ClusteredCache} to this ClusteredCacheManager.
   * <P/>
   * If a {@link com.terracotta.entity.ehcache.ClusteredCache} with that name is already known, it will be returned and
   * the clusteredCache passed in will not be added.
   *
   * @param cacheName the name of the cache
   * @param clusteredCache the clustered cache
   *
   * @return the current known mapping if any, {@code null} otherwise
   */
  ClusteredCache addCacheIfAbsent(String cacheName, ClusteredCache clusteredCache);

  /**
   * Method for destroying a cache entity
   * <P/>
   * This method will follow these steps:
   * <ol>
   *   <li>Put cache entity in exclusive maintenance mode</li>
   *   If this fails, throws {@link java.lang.IllegalStateException}
   *   <li>Verify the cache entity passed in matches the current known cache entity</li>
   *   If this fails, throws {@link java.lang.IllegalArgumentException}
   *   <li>Update the cache entity state to {@link com.terracotta.entity.ClusteredEntityState#DESTROY_IN_PROGRESS} and save it</li>
   *   If this fails, throws {@link java.lang.UnsupportedOperationException}
   *   <li>Perform the destroy operation</li>
   * </ol>
   *
   * @param clusteredCache the cache entity to destroy
   * @return {@code true} if entity was effectively destroyed, {@code false} if entity does not exist
   */
  boolean destroyCache(ClusteredCache clusteredCache);

  ToolkitReadWriteLock getCacheLock(String cacheName);

  void markInUse();

  void releaseUse();

  boolean isUsed();

  void markCacheInUse(ClusteredCache clusteredCache);

  void releaseCacheUse(ClusteredCache clusteredCache);

  boolean isCacheUsed(ClusteredCache clusteredCache);

}
