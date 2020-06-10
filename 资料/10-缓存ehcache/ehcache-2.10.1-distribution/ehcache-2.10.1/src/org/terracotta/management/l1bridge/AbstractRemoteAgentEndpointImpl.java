/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.l1bridge;

import org.terracotta.management.ServiceLocator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Sample implementation of {@link RemoteAgentEndpoint} that perform method calls
 * on services registered in the {@link ServiceLocator}.
 *
 * @author Ludovic Orban
 */
public abstract class AbstractRemoteAgentEndpointImpl implements RemoteAgentEndpoint {

  @Override
  public byte[] invoke(RemoteCallDescriptor remoteCallDescriptor) throws Exception {
    String serviceName = remoteCallDescriptor.getServiceName();
    try {
      Class<?> serviceClass = Class.forName(serviceName);

      Object service = ServiceLocator.locate(serviceClass);
      if (service == null) {
        throw new Exception("No such service registered in ServiceLocator: " + serviceName);
      }

      Method method = service.getClass()
          .getMethod(remoteCallDescriptor.getMethodName(), remoteCallDescriptor.getParamClasses());

      Object returnValue = method.invoke(service, remoteCallDescriptor.getParams());
      return serialize(returnValue);
    } catch (ClassNotFoundException cnfe) {
      throw new Exception("Service class does not exist: " + serviceName, cnfe);
    } catch (NoSuchMethodException nsme) {
      throw new Exception("Service does not implement method " + fullMethodName(remoteCallDescriptor), nsme);
    } catch (IllegalAccessException iae) {
      throw new Exception("Error accessing method " + fullMethodName(remoteCallDescriptor), iae);
    } catch (InvocationTargetException ite) {
      throw new Exception("Error invoking remote method " + fullMethodName(remoteCallDescriptor), ite);
    } catch (IOException ioe) {
      throw new Exception("Error serializing return value of " + fullMethodName(remoteCallDescriptor), ioe);
    }
  }

  private static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    try {
      oos.writeObject(obj);
    } finally {
      oos.close();
    }
    return baos.toByteArray();
  }

  private static String fullMethodName(RemoteCallDescriptor remoteCallDescriptor) {
    return remoteCallDescriptor.getServiceName() + "." + remoteCallDescriptor.getMethodName() + "()";
  }

}
