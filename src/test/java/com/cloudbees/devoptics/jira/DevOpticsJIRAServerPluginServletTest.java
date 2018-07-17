package com.cloudbees.devoptics.jira;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbees.devoptics.jira.DevOpticsJIRAServerPluginServlet;
import com.cloudbees.devoptics.jira.DevOpticsJIRAServerPluginServlet.AuthenticationContext;
import com.cloudbees.devoptics.jira.DevOpticsJIRAServerPluginServlet.HtmlContentProvider;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.Answers;

public class DevOpticsJIRAServerPluginServletTest {

    private static final String API_PATH = "/plugins/servlet/devoptics";
    private static final String CLICKJACKING_PROTECTION_EXCLUDE_KEY = "com.atlassian.jira.clickjacking.protection.exclude";

    private DevOpticsJIRAServerPluginServlet servlet = new DevOpticsJIRAServerPluginServlet();

    @Test
    public void testClickjackingProtectionExclude_missingProperty() throws Exception {

        // The key doesn't exist.
        System.getProperties().remove(CLICKJACKING_PROTECTION_EXCLUDE_KEY);

        // Initialise the servlet
        servlet.init();

        // Get the system property value and check out servlet's path has been added to it.
        final Object value = System.getProperties().get(CLICKJACKING_PROTECTION_EXCLUDE_KEY).toString();
        assertEquals(API_PATH, value);
    }

    @Test
    public void testClickjackingProtectionExclude_withValue() throws Exception {

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
        testClickjackingProtectionExclude_withValue("", "/plugins/servlet/devoptics");
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

    @Test
    public void testDoGet_blankPath() throws IOException, ServletException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        servlet = new DevOpticsJIRAServerPluginServlet(null, null);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn(null);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    public void testDoGet_doLogin_isLoggedIn() throws ServletException, IOException {

        // Setup test objects
        final String returnUrl = "some URL to return to";
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/do-login");
        when(authenticationContext.isLoggedInUser()).thenReturn(true);
        when(request.getSession().getAttribute("devOpticsReturnUrl")).thenReturn(returnUrl);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response).sendRedirect(returnUrl);
    }

    @Test
    public void testDoGet_doLogin_isNotLoggedIn() throws ServletException, IOException {

        // Setup test objects
        final String requestUri = "the request URI";
        final String returnUrl = "some URL to return to";
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/do-login");
        when(authenticationContext.isLoggedInUser()).thenReturn(false);
        when(request.getParameter("devOpticsReturnUrl")).thenReturn(returnUrl);
        when(request.getRequestURI()).thenReturn(requestUri);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(request.getSession()).setAttribute("devOpticsReturnUrl", returnUrl);
        verify(response).sendRedirect("/login.jsp?os_destination=" + URLEncoder.encode(requestUri, StandardCharsets.UTF_8.name()));
    }

    @Test
    public void testDoGet_doLogin_noReturnPath() throws ServletException, IOException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/do-login");
        when(authenticationContext.isLoggedInUser()).thenReturn(false);
        when(request.getParameter("devOpticsReturnUrl")).thenReturn(null);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response.getWriter()).println("Missing devOpticsReturnUrl query parameter");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }

    @Test
    public void testDoGet_doLogin_noSessionAttribute() throws ServletException, IOException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/do-login");
        when(authenticationContext.isLoggedInUser()).thenReturn(true);
        when(request.getSession().getAttribute("devOpticsReturnUrl")).thenReturn(null);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    @Test
    public void testDoGet_isLoggedIn_false() throws IOException, ServletException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/is-logged-in");
        when(authenticationContext.isLoggedInUser()).thenReturn(false);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response.getWriter()).print("No");
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    public void testDoGet_isLoggedIn_true() throws IOException, ServletException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final AuthenticationContext authenticationContext = mock(AuthenticationContext.class);
        servlet = new DevOpticsJIRAServerPluginServlet(null, authenticationContext);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/is-logged-in");
        when(authenticationContext.isLoggedInUser()).thenReturn(true);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response.getWriter()).print("Yes");
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void testDoGet_loadHtml() throws ServletException, IOException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        final byte[] htmlContent = "this is some HTML content".getBytes(StandardCharsets.UTF_8);
        final HtmlContentProvider htmlContentProvider = mock(HtmlContentProvider.class);
        servlet = new DevOpticsJIRAServerPluginServlet(htmlContentProvider, null);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/load.html");
        when(htmlContentProvider.getContent()).thenReturn(htmlContent);

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response).addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        verify(response).addHeader("Pragma", "no-cache");
        verify(response).addHeader("Expires", "0");
        verify(response.getOutputStream()).write(htmlContent);
        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    @Test
    public void testDoGet_unknownPath() throws ServletException, IOException {

        // Setup test objects
        final HttpServletRequest request = mock(HttpServletRequest.class, Answers.RETURNS_DEEP_STUBS);
        final HttpServletResponse response = mock(HttpServletResponse.class, Answers.RETURNS_DEEP_STUBS);
        servlet = new DevOpticsJIRAServerPluginServlet(null, null);

        // Define mock behaviour
        when(request.getPathInfo()).thenReturn("/not supported");

        // Execute test
        servlet.doGet(request, response);

        // Verify results
        verify(response).setStatus(HttpServletResponse.SC_NOT_FOUND);
    }

    private void testClickjackingProtectionExclude_withValue(final String startingValue) throws Exception {

        // Set the system property to a starting value
        System.getProperties().setProperty(CLICKJACKING_PROTECTION_EXCLUDE_KEY, startingValue);

        // Initialise the servlet
        servlet.init();

        // Get the system property value and check it is still the same as the starting value
        final String actualValue = System.getProperties().get(CLICKJACKING_PROTECTION_EXCLUDE_KEY).toString();
        assertEquals(startingValue, actualValue);
    }

    private void testClickjackingProtectionExclude_withValue(final String startingValue, final String expectedValue) throws Exception {

        System.getProperties().setProperty(CLICKJACKING_PROTECTION_EXCLUDE_KEY, startingValue);

        // Initialise the servlet
        servlet.init();

        // Get the system property value and check it has been modified to the expected value.
        final Object value = System.getProperties().get(CLICKJACKING_PROTECTION_EXCLUDE_KEY).toString();
        assertEquals(expectedValue, value);
    }
}
