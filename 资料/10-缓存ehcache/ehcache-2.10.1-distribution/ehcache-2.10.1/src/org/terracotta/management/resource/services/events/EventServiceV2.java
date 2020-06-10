/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.management.resource.services.events;

import org.terracotta.management.resource.events.EventEntityV2;

/**
 * An interface for registering topology event listeners.

 * @author Ludovic Orban
 */
public interface EventServiceV2 {

  /**
   * The interface that must be implemented by event listeners.
   */
  interface EventListener {
    /**
     * Event callback.
     * @param eventEntity the event.
     */
    void onEvent(EventEntityV2 eventEntity);

    /**
     * Error callback.
     * @param throwable a throwable representing the error.
     */
    void onError(Throwable throwable);
  }

  /**
   * Register a listener for event notifications.
   *
   * @param listener the listener.
   * @param localOnly if only local events should be listened to, in case the event source can aggregate multiple sources.
   */
  void registerEventListener(EventListener listener, boolean localOnly);

  /**
   * Unregister a previously registered listener.
   *
   * @param listener the listener.
   */
  void unregisterEventListener(EventListener listener);

}
