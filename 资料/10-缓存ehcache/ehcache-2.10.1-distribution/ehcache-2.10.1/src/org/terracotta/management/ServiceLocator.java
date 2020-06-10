/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Implements a dynamic service locator, (see
 * http://martinfowler.com/articles/injection.html#UsingAServiceLocator)
 * 
 * The intended use case is the following :
 *   1) Initialization
 *   ServiceLocator locator = new ServiceLocator();
 *   ConfigServiceImpl configServiceImpl = new ConfigServiceImpl();
 *   AgentEntity agentEntity = new AgentEntity();
 *   locator.loadService(ConfigService.class, configServiceImpl).loadService(Serializable.class, agentEntity);
 *   ServiceLocator.load(locator);
 *   
 *   2) Use of the service locator in the same class loader
 *   ConfigService value = ServiceLocator.locate(ConfigService.class);
 * 
 * You can not load it more than once (unless you call unload()); you can not start locating before load() was called
 * 
 * 
 */
public class ServiceLocator {

  private static final AtomicReference<Map<Class<?>, Object>> installedServices = new AtomicReference<Map<Class<?>, Object>>();

  private final Map<Class<?>, Object> services = new HashMap<Class<?>, Object>();

  public final <T> ServiceLocator loadService(Class<T> clazz, T implementation) {
    services.put(clazz, implementation);
    return this;
  }

  public static <T> T locate(Class<T> typeToReturn) {
    Map<Class<?>, Object> m = installedServices.get();
    if (m == null) {
      throw new IllegalStateException("The service locator has not been initialized yet ! (through the load() method)");
    }
    else {
      return (T) m.get(typeToReturn);
    }
  }

  public static void load(ServiceLocator locator) {
    // no special need for an immutable map, since installedServices is only accessed for read
    Map<Class<?>, Object> services = Collections.unmodifiableMap(new HashMap<Class<?>, Object>(locator.services));
    if (!installedServices.compareAndSet(null, services)) {
      throw new IllegalStateException("The service locator has already been initialized (through the load() method)");
    }
  }

  public static void unload() {
    installedServices.set(null);
  }
}

