/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity.internal;

import org.terracotta.toolkit.concurrent.locks.ToolkitReadWriteLock;

/**
 * InternalRootEntity
 */
public interface InternalRootEntity {

  ToolkitReadWriteLock getEntityLock();

  void destroy();

  void markDestroying();

  void alive();
}
