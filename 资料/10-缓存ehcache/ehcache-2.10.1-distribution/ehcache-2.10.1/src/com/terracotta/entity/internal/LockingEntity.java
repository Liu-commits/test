package com.terracotta.entity.internal;

import com.terracotta.entity.EntityLockHandler;

/**
 * LockingEntity
 */
public interface LockingEntity {
  void setEntityLockHandler(EntityLockHandler entityLockHandler);
}
