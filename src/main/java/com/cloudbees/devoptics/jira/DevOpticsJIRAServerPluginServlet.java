package com.cloudbees.devoptics.jira;

import com.atlassian.jira.component.ComponentAccessor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class DevOpticsJIRAServerPluginServlet extends HttpServlet {

    /* Functional interface for providing the yes/no as to whether a user is logged in or not.
     * Abstracted out to make the class more testable. */
    static interface AuthenticationContext {
        boolean isLoggedInUser();
    }

    /* Functional interface for providing the HTML content to return.
     * Abstracted out the make the class more testable. */
    static interface HtmlContentProvider {
        byte[] getContent() throws IOException;
    }

    private static final String API_PATH = "/plugins/servlet/devoptics";
    /* This is basically matching "/plugins/servlet/devoptics", but as long as it isn't followed by slashes, alphanumerics, hyphens or underscores.
     * It is allowed to be followed by the end-of-line though. */
    private static final String API_PATH_REGEX = API_PATH + "($|[^/0-9a-zA-Z_-])";
    private static final String CLICKJACKING_PROTECTION_EXCLUDE_KEY = "com.atlassian.jira.clickjacking.protection.exclude";
    private static final String DEV_OPTICS_RETURN_URL = "devOpticsReturnUrl";
    private static final String HTML_TO_SERVE = "serve-me.html";
    private static final Logger LOG = Logger.getLogger(DevOpticsJIRAServerPluginServlet.class);
    private static final String PATH_DO_LOGIN = "/do-login";
    private static final String PATH_IS_LOGGED_IN = "/is-logged-in";
    private static final String PATH_LOAD_HTML = "/load.html";
    private static final long serialVersionUID = 1L;

    private AuthenticationContext authenticationContext;
    private HtmlContentProvider htmlContentProvider;

    public DevOpticsJIRAServerPluginServlet() {
        this(
                // Default impl of the HtmlContentProvider functional interface
                () -> {
                    try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(HTML_TO_SERVE)) {
                        LOG.debug("Loading HTML content from " + HTML_TO_SERVE);
                        return IOUtils.toByteArray(is);
                    }
                },
                // Default impl of the AuthenticationContext functional interface
                () -> {
                    final boolean isLoggedInUser = ComponentAccessor.getJiraAuthenticationContext().isLoggedInUser();
                    LOG.debug("isLoggedInUser = " + isLoggedInUser);
                    return isLoggedInUser;
                });
    }

    /* For unit testing */
    DevOpticsJIRAServerPluginServlet(final HtmlContentProvider htmlContentProvider, final AuthenticationContext authenticationContext) {

        this.htmlContentProvider = htmlContentProvider;
        this.authenticationContext = authenticationContext;
    }

    @Override
    public void init() throws ServletException {

        LOG.debug("Initialising");

        /* As soon as this controller is loaded, whitelist our API. */
        configureClickjackingProtectionExcludeProperty();
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        LOG.debug("Handling request to " + req.getRequestURL() + " with query parameters " + req.getQueryString());

        // Get the requested path.  The bit that comes after /plugins/servlet/devoptics/...
        final String path = req.getPathInfo();

        if (StringUtils.isBlank(path)) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        switch (path) {
            case PATH_LOAD_HTML: {
                doLoadHtml(req, resp);
                break;
            }
            case PATH_IS_LOGGED_IN: {
                doIsLoggedIn(req, resp);
                break;
            }
            case PATH_DO_LOGIN: {
                doLogIn(req, resp);
                break;
            }
            default: {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                break;
            }
        }
    }

    private void configureClickjackingProtectionExcludeProperty() {

        // Get the current value of the click-jacking protection exclude property.
        String value = System.getProperty(CLICKJACKING_PROTECTION_EXCLUDE_KEY);
        LOG.debug("Current value of " + CLICKJACKING_PROTECTION_EXCLUDE_KEY + " property is '" + value + "'");

        if (StringUtils.isBlank(value)) {
            // The key or the key's values is missing.  Add our API.
            System.setProperty(CLICKJACKING_PROTECTION_EXCLUDE_KEY, API_PATH);
            LOG.debug("Setting value of " + CLICKJACKING_PROTECTION_EXCLUDE_KEY + " property to '" + API_PATH + "'");
        } else {
            // The key's value exists.  Next we need to check if our API is already in there.  Don't re-add ourselves if we are.
            final Pattern pattern = Pattern.compile(API_PATH_REGEX);
            final Matcher matcher = pattern.matcher(value);
            if (!matcher.find()) {
                // No match. Add ourselves
                value += "," + API_PATH;
                System.setProperty(CLICKJACKING_PROTECTION_EXCLUDE_KEY, value);
                LOG.debug("Modifying value of " + CLICKJACKING_PROTECTION_EXCLUDE_KEY + " property to '" + value + "'");
            }
        }
    }

    private void doIsLoggedIn(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

        if (!authenticationContext.isLoggedInUser()) {
            /* The user is not logged in. */
            resp.getWriter().print("No");
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        } else {
            /* The user is logged in. */
            resp.getWriter().print("Yes");
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private void doLoadHtml(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

        // Set some cache control headers
        resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.addHeader("Pragma", "no-cache");
        resp.addHeader("Expires", "0");

        // Load the HTML content in to the response
        final byte[] byteArray = htmlContentProvider.getContent();
        resp.getOutputStream().write(byteArray);
        resp.setStatus(HttpServletResponse.SC_OK);
    }

    private void doLogIn(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {

        // Expecting a URL like {jira host}/plugins/servlet/devoptics/do-login?devOpticsReturnUrl=https://devoptics.cloudbees.com/u/

        if (!authenticationContext.isLoggedInUser()) {

            /* The user is not logged in. */

            // Capture the value of the devOpticsReturnUrl query param so we know where to redirect back to.
            final String devOpticsReturnUrl = req.getParameter(DEV_OPTICS_RETURN_URL);
            if (StringUtils.isNotBlank(devOpticsReturnUrl)) {
                req.getSession().setAttribute(DEV_OPTICS_RETURN_URL, devOpticsReturnUrl);
            } else {
                // Didn't get a devOpticsReturnUrl query param.  Bad request.
                resp.getWriter().println("Missing " + DEV_OPTICS_RETURN_URL + " query parameter");
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }

            // Redirect to Jira's login, requesting to come back here again on successful login.
            final String returnUrl = req.getRequestURI();
            final String redirectTo = "/login.jsp?os_destination=" + URLEncoder.encode(returnUrl, StandardCharsets.UTF_8.name());
            LOG.debug("Redirecting to " + redirectTo);
            resp.sendRedirect(redirectTo);

        } else {

            /* The user is logged in. */

            // Redirect to wherever we were told to go in the devOpticsReturnUrl query param from the initial request.
            final String devOpticsReturnUrl = (String) req.getSession().getAttribute(DEV_OPTICS_RETURN_URL);
            if (StringUtils.isNotBlank(devOpticsReturnUrl)) {
                LOG.debug("Redirecting to " + devOpticsReturnUrl);
                resp.sendRedirect(devOpticsReturnUrl);
            } else {
                resp.getWriter().println("Did not find the " + DEV_OPTICS_RETURN_URL + " session attribute");
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }
}