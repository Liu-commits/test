/* All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.*/

package org.terracotta.management.embedded;

import java.util.EnumSet;
import java.util.EventListener;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContextListener;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.ssl.SslSelectChannelConnector;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import org.glassfish.jersey.servlet.ServletContainer;

/**
 * <p>A standalone server implementation for agents embedded at the Terracotta monitorable entity.</p>
 *
 * @author Ludovic Orban
 * @author brandony
 */
public final class StandaloneServer implements StandaloneServerInterface {
  public static final String EMBEDDED_CTXT = "/tc-management-api";

  private final List<FilterDetail> filterDetails;

  private final List<ServletContextListener> servletListeners;

  private volatile Server server;

  private final String applicationClassName;

  private final String host;

  private final int port;

  private final SSLContext sslCtxt;

  private final boolean needClientAuth;

  /**
   * Create a standalone management server.
   * @param filterDetails a list of {@link FilterDetail} to add. Can be null;
   * @param servletListeners a list of {@link ServletContextListener} to add. Can be null.
   * @param applicationClassName the {@link javax.ws.rs.core.Application} implementation to deploy.
   * @param host the host or IP address to bind. Mandatory unless port is < 0.
   * @param port the port to bind. Can be < 0 to mean do not bind.
   * @param sslCtxt the {@link SSLContext} to use. Can be null if no SSL is desired.
   * @param needClientAuth true to mandate client SSL auth, false otherwise.
   */
  public StandaloneServer(List<FilterDetail> filterDetails, List<ServletContextListener> servletListeners, String applicationClassName,
      String host, int port, SSLContext sslCtxt, boolean needClientAuth) {
    super();
    this.filterDetails = filterDetails;
    this.servletListeners = servletListeners;
    this.applicationClassName = applicationClassName;
    this.host = host;
    this.port = port;
    this.sslCtxt = sslCtxt;
    this.needClientAuth = needClientAuth;
  }


  @Override
  public void start() throws Exception {
    if (port < 0) {
      return;
    }
    if (port == 0) {
      throw new IllegalArgumentException("port must be set");
    }
    if (applicationClassName == null) {
      throw new IllegalArgumentException("applicationClassName must be set");
    }
    if (server != null) {
      throw new IllegalStateException("server already started");
    }

    try {
      server = new Server(port);

      SelectChannelConnector connector;

      if (sslCtxt != null) {
        SslContextFactory sslCtxtFact = new SslContextFactory();
        sslCtxtFact.setSslContext(sslCtxt);
        sslCtxtFact.setNeedClientAuth(needClientAuth);
        connector = new SslSelectChannelConnector(sslCtxtFact);
      } else {
        connector = new SelectChannelConnector();
      }

      connector.setHost(host);
      connector.setPort(port);
      server.setConnectors(new Connector[]{connector});


      ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath(EMBEDDED_CTXT);
      server.setHandler(context);

      ServletHolder servletHolder = new ServletHolder(new ServletContainer());
      // make sure com.sun.jersey.core.util.FeaturesAndProperties.FEATURE_XMLROOTELEMENT_PROCESSING is set to true
      // so that a list of @XmlRootElement(name = "configuration") is <configurations>
      servletHolder.setInitParameter("com.sun.jersey.config.feature.XmlRootElementProcessing", "true");
      servletHolder.setInitParameter("javax.ws.rs.Application", applicationClassName);
      // not needed anymore thanks to the jackson-jaxrs-json-provider
      // servletHolder.setInitParameter("com.sun.jersey.api.json.POJOMappingFeature", "true");
      servletHolder.setInitParameter("com.sun.jersey.spi.container.ContainerRequestFilters",
          "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
      servletHolder.setInitParameter("com.sun.jersey.spi.container.ContainerResponseFilters",
          "com.sun.jersey.api.container.filter.GZIPContentEncodingFilter");
      context.addServlet(servletHolder, "/*");

      if (servletListeners != null) {
        context.setEventListeners(servletListeners.toArray(new EventListener[] {}));
      }

      if (filterDetails != null) {
        for (FilterDetail f : filterDetails) {
          FilterHolder filterHolder = new FilterHolder(f.getFilter());
          EnumSet<DispatcherType> dTypes = null;

          if (f.getDispatcherNames() != null) {
            dTypes = EnumSet.noneOf(DispatcherType.class);

            for (String dn : f.getDispatcherNames()) {
              dTypes.add(DispatcherType.valueOf(dn));
            }
          }

          context.addFilter(filterHolder, f.getPathSpec(), dTypes);
        }
      }

      server.start();
    } catch (Exception e) {
      server.stop();
      server = null;
      throw e;
    }
  }

  @Override
  public void stop() throws Exception {
    if (server == null || port < 0) {
      return;
    }

    try {
      server.stop();
      server.join();
    } finally {
      server = null;
    }
  }
}