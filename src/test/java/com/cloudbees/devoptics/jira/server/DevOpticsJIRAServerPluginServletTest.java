package com.cloudbees.devoptics.jira.server;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.lang.reflect.Method;

import org.junit.Test;

public class DevOpticsJIRAServerPluginServletTest {

    //com.atlassian.jira.clickjacking.protection.exclude=/rest/my-plugin/1.0/dashboard,/rest/collectors/1.0/template/form/

    private static final class TestClassLoader extends ClassLoader {

        private void load() throws Exception {

            // Load the class
            final InputStream fileInputStream = this.getClass().getResourceAsStream("DevOpticsJIRAServerPluginServlet.class");
            final byte rawBytes[] = new byte[fileInputStream.available()];
            fileInputStream.read(rawBytes);
            final Class<?> clazz = this.defineClass(null, rawBytes, 0, rawBytes.length);

            // Invoke the initialise method
            final Method method = clazz.getDeclaredMethod("configureClickjackingProtectionExcludeProperty");
            method.setAccessible(true);
            method.invoke(null);
        }
    }

    private static final String KEY = "com.atlassian.jira.clickjacking.protection.exclude";
    private static final String VALUE = "/plugins/servlet/devoptics";

    @Test
    public void testClickjackingProtectionExclude_missingProperty() throws ClassNotFoundException {

        // The key doesn't exist.
        System.getProperties().remove(KEY);

        // Load the servlet class
        invokeConfigureClickjackingProtectionExcludeProperty();

        // Get the system property value and check out servlet's path has been added to it.
        final Object value = System.getProperties().get(KEY).toString();
        assertEquals(VALUE, value);
    }

    @Test
    public void testClickjackingProtectionExclude_missingPropertyValue() throws ClassNotFoundException {

        // The key's value doesn't exist.
        System.getProperties().setProperty(KEY, "");

        // Load the servlet class
        invokeConfigureClickjackingProtectionExcludeProperty();

        // Get the system property value and check out servlet's path has been added to it.
        final Object value = System.getProperties().get(KEY).toString();
        assertEquals(VALUE, value);
    }

    @Test
    public void testClickjackingProtectionExclude_withValue() throws ClassNotFoundException {

        // These shouldn't get modified
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue(" /plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devoptics,bar");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devoptics ,bar");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devoptics ,bar");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devoptics,bar");

        // These should get modified.
        testClickjackingProtectionExclude_withValue("foo", "foo,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devoptics/x", "foo,/plugins/servlet/devoptics/x,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devoptics/x", "foo, /plugins/servlet/devoptics/x,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devoptics/x,bar", "foo,/plugins/servlet/devoptics/x,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devoptics/x ,bar",
                "foo, /plugins/servlet/devoptics/x ,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devoptics/x ,bar", "/plugins/servlet/devoptics/x ,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devoptics/x,bar", "/plugins/servlet/devoptics/x,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devopticsx", "foo,/plugins/servlet/devopticsx,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devopticsx", "foo, /plugins/servlet/devopticsx,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo,/plugins/servlet/devopticsx,bar", "foo,/plugins/servlet/devopticsx,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("foo, /plugins/servlet/devopticsx ,bar",
                "foo, /plugins/servlet/devopticsx ,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devopticsx ,bar", "/plugins/servlet/devopticsx ,bar,/plugins/servlet/devoptics");
        testClickjackingProtectionExclude_withValue("/plugins/servlet/devopticsx,bar", "/plugins/servlet/devopticsx,bar,/plugins/servlet/devoptics");

    }

    private void invokeConfigureClickjackingProtectionExcludeProperty() {

        try {
            final Method method = DevOpticsJIRAServerPluginServlet.class.getDeclaredMethod("configureClickjackingProtectionExcludeProperty");
            method.setAccessible(true);
            method.invoke(null);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }

        //        loadServletClass();
    }

    private void loadServletClass() {

        try {
            new TestClassLoader().load();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void testClickjackingProtectionExclude_withValue(final String startingValue) throws ClassNotFoundException {

        // Set the system property to a starting value
        System.getProperties().setProperty(KEY, startingValue);

        // Load the servlet class
        invokeConfigureClickjackingProtectionExcludeProperty();

        // Get the system property value and check it has not been modified.
        final String actualValue = System.getProperties().get(KEY).toString();
        assertEquals(startingValue, actualValue);
    }

    private void testClickjackingProtectionExclude_withValue(final String startingValue, final String expectedValue) throws ClassNotFoundException {

        System.getProperties().setProperty(KEY, startingValue);

        // Load the servlet class
        invokeConfigureClickjackingProtectionExcludeProperty();

        // Get the system property value and check out servlet's path has been added to it.
        final Object value = System.getProperties().get(KEY).toString();
        assertEquals(expectedValue, value);
    }
}
