/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */

package org.terracotta.management.resource.services.validator;

import javax.ws.rs.core.UriInfo;

/**
 * A validator interface for request to REST resources.
 *
 * @author brandony
 */
public interface RequestValidator {

  /**
   * A validator for safe Http methods.
   *
   * @param info for this request
   */
  void validateSafe(UriInfo info);

  /**
   * A validator for unsafe Http methods.
   *
   * @param info for this request
   */
  void validate(UriInfo info);
}
