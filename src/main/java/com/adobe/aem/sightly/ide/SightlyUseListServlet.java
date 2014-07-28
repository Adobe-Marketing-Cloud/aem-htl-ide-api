/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package com.adobe.aem.sightly.ide;

/**
 * SightlyUseListServlet
 * <p/>
 *      this servlet gives a list of all Use beans used in that instance
 * <p/>
 * user : npeltier
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SlingServlet(resourceTypes={"sling/servlet/default"}, methods={"GET"}, selectors = {"sightlyBeans"}, extensions = {"json"})
public class SightlyUseListServlet extends SlingSafeMethodsServlet {

    private Logger logger = LoggerFactory.getLogger(SightlyUseListServlet.class);

    @Reference
    protected SightlyBeanFinder sightlyBeanFinder;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding("utf8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(HttpServletResponse.SC_OK);
            JSONWriter w = new JSONWriter(response.getWriter());
            sightlyBeanFinder.writeUseBeans(w);
        } catch (Exception e){
            throw new ServletException(e);
        }
    }

}
