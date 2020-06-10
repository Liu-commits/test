/*
 * All content copyright (c) 2003-2012 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.management.embedded;

import org.terracotta.management.resource.ErrorEntity;
import org.terracotta.management.resource.exceptions.ExceptionUtils;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet filter that prevents Identity Assertion requests from being processed. This is
 * useful to prevent non-secure agents from processing secure requests.
 *
 * @author Ludovic Orban
 */
public class NoIaFilter implements Filter {

    private static final String REQ_TICKET = "req-ticket";
    private static final String TC_ID_TOKEN = "tc-identity-token";
    private static final String ALIAS = "alias";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest)request;
        HttpServletResponse httpServletResponse = (HttpServletResponse)response;

        if (httpServletRequest.getHeader(REQ_TICKET) != null ||
                httpServletRequest.getHeader(TC_ID_TOKEN) != null ||
                httpServletRequest.getHeader(ALIAS) != null) {
            httpServletResponse.setContentType("application/json");
      ErrorEntity jsonError = ExceptionUtils.toErrorEntity(new ServletException(
          "Request cannot contain security headers"));
            httpServletResponse.getWriter().print(jsonError.toJSON());
            httpServletResponse.setStatus(400);
            return;
        }

        chain.doFilter(request, response);
    }

}
