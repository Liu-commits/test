/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.application;

import org.glassfish.jersey.media.sse.SseFeature;
import org.terracotta.management.resource.services.AgentsResourceServiceImplV2;
import org.terracotta.management.resource.services.events.AllEventsResourceServiceImplV2;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that adds the commonly used
 * resources and providers.
 * @author Ludovic Orban
 */
public class DefaultApplicationV2 extends DefaultApplicationCommon {

  /**
   * Get a default set of resource and provider classes.
   * @return a default set of classes.
   */
  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> s = new HashSet<Class<?>>(super.getClasses());
    s.add(AgentsResourceServiceImplV2.class);
    s.add(AllEventsResourceServiceImplV2.class);

    // sse feature is v2 only
    s.add(SseFeature.class);

    return s;
  }

}
