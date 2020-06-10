/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity;

import java.io.Serializable;

/**
 * ClusteredEntity
 */
public interface ClusteredEntity<T extends EntityConfiguration> extends Serializable {

  T getConfiguration();

  ClusteredEntityState getState();

}
