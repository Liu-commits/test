package org.terracotta.management.embedded;

/**
 * 
 * The interface representing the Standalone Server, used to configure and start/stop an underlying application server
 * 
 * @author Anthony Dahanne
 * 
 */
public interface StandaloneServerInterface {

  void stop() throws Exception;

  void start() throws Exception;

}
