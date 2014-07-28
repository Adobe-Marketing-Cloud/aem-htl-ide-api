/*******************************************************************************
 * Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
 *
 * Licensed under the Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 ******************************************************************************/
package com.adobe.aem.sightly.ide.impl;

import com.adobe.cq.sightly.WCMUse;
import com.adobe.aem.sightly.ide.SightlyBeanFinder;
import java.io.*;
import java.lang.reflect.Modifier;
import java.util.*;
import javax.jcr.query.Query;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.io.JSONWriter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.MethodInfo;
import org.clapper.util.classutil.SubclassClassFilter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SightlyBeanFinderImpl
 */
@Component
@Service
@Properties({
        @Property(name= EventConstants.EVENT_TOPIC,value= {SlingConstants.TOPIC_RESOURCE_ADDED,SlingConstants.TOPIC_RESOURCE_CHANGED, SlingConstants.TOPIC_RESOURCE_REMOVED}),
        @Property(name=EventConstants.EVENT_FILTER,value="(path=/apps/*/install/*.jar)")
})
public class SightlyBeanFinderImpl implements SightlyBeanFinder, EventHandler {
    Logger log = LoggerFactory.getLogger(this.getClass());

    Map<String,ClassInfo> beans;

    private static final String GETTER_PREFIX = "get";
    private static final String IS_PREFIX = "is";

    public static final String KEY_METHOD_NAME = "name";
    public static final String KEY_METHOD_DESC = "desc";
    public static final String KEY_METHOD_SIGN = "sign";


    private String topClass = "com.adobe.cq.sightly.WCMUse";

    @Reference
    ResourceResolverFactory factory;

    @Activate
    public void activate(){
        loadClassFinder();
    }

    private Comparator<ClassInfo> beanComparator = new Comparator<ClassInfo>() {

        @Override
        public int compare(final ClassInfo o1, final ClassInfo o2) {
            return o1.getClassName().compareTo(o2.getClassName());
        }
    };

    private Comparator<JSONObject> memberComparator = new Comparator<JSONObject>() {

        @Override
        public int compare(final JSONObject o1, final JSONObject o2) {
            return o1.optString(KEY_METHOD_NAME,"").compareTo(o2.optString(KEY_METHOD_NAME,""));
        }
    };

    private void fillTempFile(File file, Resource resource) throws IOException {
        InputStream inputStream = null;
        try {
            inputStream = resource.adaptTo(InputStream.class);
            // write the inputStream to a FileOutputStream
            OutputStream outputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, outputStream);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    /**
     *
     */
    private void loadClassFinder(){
        log.info("starting looking for bundles...");
        ResourceResolver resolver = null;
        Collection<File> files = new ArrayList<File>();
        try {
            resolver = factory.getAdministrativeResourceResolver(null);
            Iterator<Resource> jarResources = resolver.findResources("//element(*,nt:file)[jcr:contains(., '.jar')]", Query.XPATH);
            Collection<String> paths = new ArrayList<String>();
            while (jarResources.hasNext()){
                Resource jarResource = jarResources.next();
                String path = jarResource.getPath();
                if ((path.startsWith("/apps") || path.contains("/sightly/install/"))){
                    paths.add(path);
                }
            }
            log.debug("will parse following libs : {}", paths);
            ClassFinder classFinder = new ClassFinder();
            for (String path : paths){
                Resource jarResource = resolver.getResource(path);
                log.debug("parsing {}", jarResource.getName());
                File file = File.createTempFile("tmp",".jar");
                files.add(file);
                fillTempFile(file, jarResource);

                // read this file into InputStream
                classFinder.add(file);
            }

            Collection<ClassInfo> beansSet = new ArrayList<ClassInfo>();
            //parsing all classes & sub classes implementing Use class
            classFinder.findClasses(beansSet, new SubclassClassFilter(WCMUse.class));
            log.debug("found those classes : {}", beansSet);
            Map<String,ClassInfo> newBeans = new HashMap<String, ClassInfo>();
            for (ClassInfo info : beansSet){
                newBeans.put(info.getClassName(), info);
            }
            beans = newBeans;//we do the affectation at the very end to avoid at most sync issues.
        } catch (Exception e){
            log.error("unable to load classfinder information", e);
        } finally {
            if (resolver != null){
                resolver.close();
            }
            for (File file : files){
                file.delete();
            }
        }

    }

    /**
     * write use beans
     * @return
     */
    public void writeUseBeans(JSONWriter writer) throws JSONException {
        Collection<ClassInfo> sortedBeans = new TreeSet<ClassInfo>(beanComparator);
        sortedBeans.addAll(beans.values());
        writer.object();
        for (ClassInfo bean : sortedBeans){
            JSONObject classObject = new JSONObject();
            classObject.put("members", getMembers(bean.getClassName()));
            classObject.put("super", bean.getSuperClassName());
            writer.key(bean.getClassName()).value(classObject);
        }
        writer.endObject();
    }

    /**
     * Get the array of a beans members
     * @param name
     * @return
     */
    private JSONArray getMembers(String name) throws JSONException {
        Set<JSONObject> members = new TreeSet<JSONObject>(memberComparator);
        addMembers(members, name);
        return new JSONArray(members);
    }

    /**
     * Recursive methods for adding class & superclasses getters
     * @param members
     * @param name
     */
    private void addMembers(Set<JSONObject> members, String name) throws JSONException {
        ClassInfo bean = beans.get(name);
        for (MethodInfo methodInfo : bean.getMethods()){
            String mName = methodInfo.getName();
            if (methodInfo.getAccess() == Modifier.PUBLIC){
                String foundName = null;
                if (mName.startsWith(GETTER_PREFIX) && (mName.length() > GETTER_PREFIX.length())){
                    foundName = mName.substring(GETTER_PREFIX.length());
                } else if (mName.startsWith(IS_PREFIX) && (mName.length() > IS_PREFIX.length())){
                    foundName = mName.substring(IS_PREFIX.length());
                }
                if (foundName != null){
                    StringBuilder builder = new StringBuilder(foundName.substring(0,1).toLowerCase()).append(foundName.substring(1));
                    JSONObject object = new JSONObject();
                    object.put(KEY_METHOD_NAME, builder.toString());
                    object.put(KEY_METHOD_DESC, methodInfo.getDescription());
                    object.put(KEY_METHOD_SIGN, methodInfo.getSignature());
                    members.add(object);
                }
            }
        }
        String superClass = bean.getSuperClassName();
        if ((superClass != null) && (!superClass.equals(topClass)) && (beans.get(superClass) != null)){
            addMembers(members, bean.getSuperClassName());
        }
    }

    public void handleEvent(Event event) {
        String path = (String) event.getProperty("path");
        log.info("Detected a change at {}, rebuilding the beans information", path);
        loadClassFinder();
    }
}