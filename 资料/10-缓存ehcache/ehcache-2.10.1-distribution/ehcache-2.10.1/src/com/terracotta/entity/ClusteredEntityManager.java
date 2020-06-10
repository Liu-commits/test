/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity;

import org.terracotta.toolkit.Toolkit;
import org.terracotta.toolkit.collections.ToolkitMap;
import org.terracotta.toolkit.concurrent.locks.ToolkitLock;
import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

import com.terracotta.entity.internal.InternalRootEntity;
import com.terracotta.entity.internal.LockingEntity;
import com.terracotta.entity.internal.ToolkitAwareEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * ClusteredEntityManager
 */
public class ClusteredEntityManager {

  private static final long TRY_LOCK_TIMEOUT_SECONDS = 2;

  private final Toolkit       toolkit;
  private final EntityLockHandler entityLockHandler;

  private volatile transient ConcurrentMap<Class, ToolkitMap<String, ? extends RootEntity>> entityMapsMap = new ConcurrentHashMap<Class, ToolkitMap<String, ? extends RootEntity>>();

  public ClusteredEntityManager(Toolkit toolkit) {
    this(toolkit, new EntityLockHandler(toolkit));
  }

  ClusteredEntityManager(Toolkit toolkit, EntityLockHandler entityLockHandler) {
    this.toolkit = toolkit;
    this.entityLockHandler = entityLockHandler;
  }

  public <T extends RootEntity> T getRootEntity(String name, Class<T> entityClass) {
    T entity = getRootEntityInternal(name, entityClass);
    if (entity != null && ClusteredEntityState.DESTROY_IN_PROGRESS.equals(entity.getState())) {
      destroyRootEntitySilently(name, entityClass, entity);
      return null;
    }
    return entity;
  }

  public <T extends RootEntity> Map<String, T> getRootEntities(Class<T> entityClass) {
    HashMap<String, T> resultMap = new HashMap<String, T>();
    for (Map.Entry<String, T> entry : getEntityMap(entityClass).entrySet()) {
      T entity = entry.getValue();
      if (!ClusteredEntityState.DESTROY_IN_PROGRESS.equals(entity.getState())) {
        resultMap.put(entry.getKey(), processEntity(entity));
      } else {
        destroyRootEntitySilently(entry.getKey(), entityClass, entity);
      }
    }
    return Collections.unmodifiableMap(resultMap);
  }

  /**
   * Method for adding a {@link com.terracotta.entity.RootEntity} to this ClusteredEntityManager.
   * <P/>
   * If a {@link com.terracotta.entity.RootEntity} of the same clusteredEntityClass with the same name is already known
   * to this ClusteredEntityManager, that entity will be returned and the clusteredEntity passed in will not added.
   *
   * @param name the name of the entity
   * @param clusteredEntityClass the type of the entity
   * @param clusteredEntity the clustered entity
   *
   * @return the current known mapping if any, {@code null} otherwise
   */
  public <T extends RootEntity> T addRootEntityIfAbsent(String name, Class<T> clusteredEntityClass, T clusteredEntity) {
    ToolkitMap<String, T> map = getEntityMap(clusteredEntityClass);
    T oldValue = map.putIfAbsent(name, clusteredEntity);
    if (oldValue != null) {
      return processEntity(oldValue);
    } else {
      processEntity(clusteredEntity);
      return null;
    }
  }

  /**
   * Method for destroying a root entity
   * <P/>
   * This method will follow these steps:
   * <ol>
   *   <li>Put entity in exclusive maintenance mode</li>
   *   If this fails, throws {@link java.lang.IllegalStateException}
   *   <li>Verify the entity passed in matches the current known entity</li>
   *   If this fails, throws {@link java.lang.IllegalArgumentException}
   *   <li>Mark the entity with {@link com.terracotta.entity.ClusteredEntityState#DESTROY_IN_PROGRESS} and save it</li>
   *   If this fails, throws {@link java.lang.UnsupportedOperationException}
   *   <li>Perform the destroy operation</li>
   * </ol>
   *
   * @param name name of the entity to destroy
   * @param rootEntityClass public interface under which this entity is managed
   * @param controlEntity the entity to destroy
   * @param <T> The managed entity type
   * @return {@code true} if entity was effectively destroyed, {@code false} if entity does not exist
   */
  public <T extends RootEntity> boolean destroyRootEntity(String name, Class<T> rootEntityClass, T controlEntity) {
    InternalRootEntity currentRootEntity = asInternalRootEntity(getRootEntityInternal(name, rootEntityClass));
    if (currentRootEntity != null) {
      ToolkitLock entityWriteLock = currentRootEntity.getEntityLock().writeLock();
      try {
        if (entityWriteLock.tryLock(TRY_LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
          try {
            if (!currentRootEntity.equals(controlEntity)) {
              throw new IllegalArgumentException(String.format("The specified entity named %s does not match " +
                                                               "the mapping known to this entity manager", name));
            }
            currentRootEntity.markDestroying();
            try {
              getEntityMap(rootEntityClass).put(name, (T) currentRootEntity);
            } catch (Exception e) {
              // Failed to save entity in destroy state - abort
              currentRootEntity.alive();
              throw new UnsupportedOperationException(String.format("Unable to mark entity %s of type %s with destroy in progress", name, rootEntityClass), e);
            }
            currentRootEntity.destroy();
            getEntityMap(rootEntityClass).remove(name);
            return true;
          } finally {
            entityWriteLock.unlock();
          }
        } else {
          throw new IllegalStateException(String.format("Unable to lock entity %s of type %s for destruction", name, rootEntityClass));
        }
      } catch (InterruptedException e) {
        throw new IllegalStateException(String.format("Unable to lock entity %s of type %s for destruction", name, rootEntityClass), e);
      }
    }
    return false;
  }

  private <T extends RootEntity> InternalRootEntity asInternalRootEntity(T currentRootEntity) {
    return (InternalRootEntity) currentRootEntity;
  }

  public ToolkitReadWriteLock getEntityLock(String lockName) {
    return toolkit.getReadWriteLock(lockName);
  }

  public void dispose() {
    entityLockHandler.dispose();
  }

  private <T extends RootEntity> T getRootEntityInternal(String name, Class<T> rootEntityClass) {
    return processEntity(getEntityMap(rootEntityClass).get(name));
  }

  private <T extends RootEntity> void destroyRootEntitySilently(String name, Class<T> entityClass, T entity) {
    try {
      destroyRootEntity(name, entityClass, entity);
    } catch (Exception e) {
      // Ignore - trying to destroy left overs
    }
  }

  private <T extends RootEntity> T processEntity(T entity) {
    if (entity instanceof ToolkitAwareEntity) {
      ((ToolkitAwareEntity)entity).setToolkit(toolkit);
    }
    if (entity instanceof LockingEntity) {
      ((LockingEntity)entity).setEntityLockHandler(entityLockHandler);

    }
    return entity;
  }

  private <T extends RootEntity> ToolkitMap<String, T> getEntityMap(Class<T> entityClass) {
    ToolkitMap<String, T> entityMap = (ToolkitMap<String, T>) entityMapsMap.get(entityClass);
    if (entityMap == null) {
      entityMap = toolkit.getMap(getMapName(entityClass), String.class, entityClass);
      ToolkitMap<String, T> installedMap = (ToolkitMap<String, T>) entityMapsMap.putIfAbsent(entityClass, entityMap);
      if (installedMap != null) {
        entityMap = installedMap;
      }
    }
    return entityMap;
  }

  <T extends RootEntity> String getMapName(Class<T> entityClass) {
    return entityClass.getName();
  }
}
