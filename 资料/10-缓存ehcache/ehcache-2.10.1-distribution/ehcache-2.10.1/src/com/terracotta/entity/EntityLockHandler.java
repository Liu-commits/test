package com.terracotta.entity;

import org.terracotta.toolkit.Toolkit;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * EntityLockHandler
 */
public class EntityLockHandler {

  private final ExecutorService executorService;
  private final Toolkit toolkit;

  EntityLockHandler(Toolkit toolkit) {
    this.toolkit = toolkit;
    executorService = Executors.newSingleThreadExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "clustered-entity-locking-thread");
            thread.setDaemon(true);
            return thread;
        }
    });
  }

  public void readLock(final String lockName) {
    try {
      executorService.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          toolkit.getReadWriteLock(lockName).readLock().lock();
          return null;
        }
      }).get();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to acquire read lock for lock " + lockName, e);
    }
  }

  public void readUnlock(final String lockName) {
    try {
      executorService.submit(new Callable<Void>() {
        @Override
        public Void call() throws Exception {
          toolkit.getReadWriteLock(lockName).readLock().unlock();
          return null;
        }
      }).get();
    } catch (Exception e) {
      throw new IllegalStateException("Unable to release read lock for lock " + lockName, e);
    }
  }

  public void dispose() {
    executorService.shutdownNow();
  }
  
}
