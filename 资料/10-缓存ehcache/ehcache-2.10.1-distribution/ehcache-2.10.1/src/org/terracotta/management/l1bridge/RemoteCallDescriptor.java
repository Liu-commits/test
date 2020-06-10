/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.l1bridge;

import java.io.Serializable;

/**
 * The L1 bridge remote call descriptor contains everything that is necessary to describe a remote service call.
 *
 * @author Ludovic Orban
 */
public class RemoteCallDescriptor implements Serializable {

  private final String ticket;
  private final String token;
  private final String iaCallbackUrl;
  private final String serviceName;
  private final String methodName;
  private final Class[] paramClasses;
  private final Object[] params;

  /**
   * Create instance.
   *
   * @param ticket the security ticket, can be null if there is no security context to pass on.
   * @param token the security token, can be null if there is no security context to pass on.
   * @param iaCallbackUrl the security IA callback URL, can be null if there is no security context to pass on.
   * @param serviceName the service name on which to perform the invocation.
   * @param methodName the name of the method to invoke.
   * @param paramClasses the method parameter types.
   * @param params the method parameters.
   */
  public RemoteCallDescriptor(String ticket, String token, String iaCallbackUrl, String serviceName, String methodName, Class[] paramClasses, Object[] params) {
    this.ticket = ticket;
    this.token = token;
    this.iaCallbackUrl = iaCallbackUrl;
    this.serviceName = serviceName;
    this.methodName = methodName;
    this.paramClasses = paramClasses;
    this.params = params;
  }

  public String getTicket() {
    return ticket;
  }

  public String getToken() {
    return token;
  }

  public String getIaCallbackUrl() {
    return iaCallbackUrl;
  }

  public String getServiceName() {
    return serviceName;
  }

  public String getMethodName() {
    return methodName;
  }

  public Class[] getParamClasses() {
    return paramClasses;
  }

  public Object[] getParams() {
    return params;
  }
}
