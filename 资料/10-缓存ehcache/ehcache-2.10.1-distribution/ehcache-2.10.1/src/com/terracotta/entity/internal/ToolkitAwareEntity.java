/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.entity.internal;

import org.terracotta.toolkit.Toolkit;

/**
 * ToolkitAwareEntity
 */
public interface ToolkitAwareEntity {

  void setToolkit(Toolkit toolkit);
}
