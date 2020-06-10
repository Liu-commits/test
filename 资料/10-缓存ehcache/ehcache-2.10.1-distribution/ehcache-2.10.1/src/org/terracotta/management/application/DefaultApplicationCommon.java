/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.management.application;

import org.glassfish.jersey.message.DeflateEncoder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.filter.EncodingFilter;

import java.util.HashSet;
import java.util.Set;

/**
 * A class that adds the commonly used resources and providers.
 * 
 * @author Anthony Dahanne
 */
public class DefaultApplicationCommon {

  /**
   * Get a default set of resource and provider classes.
   * 
   * @return a default set of classes.
   */
  public Set<Class<?>> getClasses() {

    return new HashSet<Class<?>>() {{

        add(DefaultExceptionMapper.class);
        add(ResourceRuntimeExceptionMapper.class);
        add(WebApplicationExceptionMapper.class);

        // gzip compression
        add(GZipEncoder.class);
        add(EncodingFilter.class);
        add(DeflateEncoder.class);
      }
    };
  }
}
