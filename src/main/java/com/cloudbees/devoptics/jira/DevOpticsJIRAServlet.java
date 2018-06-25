package com.cloudbees.devoptics.jira;

import com.atlassian.jira.component.ComponentAccessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:tom.fennelly@gmail.com">tom.fennelly@gmail.com</a>
 */
public class DevOpticsJIRAServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final String KEY = "com.atlassian.jira.clickjacking.protection.exclude";
    private static final String VALUE = "/plugins/servlet/devoptics";
    private static final String HTML_TO_SERVE = "/home/serve-me.html";

    static {

        /* TODO: Need a better implementation than this. One that won't wipe out any existing settings or repat-add our entyr.
         * See https://confluence.atlassian.com/jirakb/security-headers-in-jira-939919914.html for more info
         */

        // As soon as this controller is loaded, whitelist our API.
        System.setProperty(KEY, VALUE);
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

        // Get the requested path.  The bit that comes after /plugins/servlet/devoptics/...
        final String path = req.getPathInfo();

        if (path == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        // http://localhost:8080/plugins/servlet/devoptics/{path}

        if (path.equals("/load.html")) {

            // Set some cache control headers
            resp.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            resp.addHeader("Pragma", "no-cache");
            resp.addHeader("Expires", "0");

            // Pull the page from an absolute place on the filesystem rather than from within the packaged jar file
            // just so that we can make changes to the served page without re-deploying the jar each time.
            final File file = new File(HTML_TO_SERVE);
            System.out.println("Serving page: " + file);

            try (InputStream inputStream = new FileInputStream(file)) {
                final byte[] byteArray = IOUtils.toByteArray(inputStream);
                resp.getOutputStream().write(byteArray);
            } catch (final Exception e) {
                // A bad thing happened.  Pass back the info to the client.
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                e.printStackTrace(resp.getWriter());
                return;
            }

            resp.setStatus(HttpServletResponse.SC_OK);
            return;

        } else if (path.equals("/is-logged-in")) {

            // Convenience check to see if the user is logged in.  The result of this can inform the
            // client as to whether they need to go ahead and call ../do-login or not.
            if (!ComponentAccessor.getJiraAuthenticationContext().isLoggedInUser()) {
                resp.getWriter().print("No");
                resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            } else {
                resp.getWriter().print("Yes");
                resp.setStatus(HttpServletResponse.SC_OK);
            }
            return;

        } else if (path.equals("/do-login")) {

            // http://localhost:8080/plugins/servlet/devoptics/do-login?devOpticsReturnUrl=https://devoptics.cloudbees.com/u/<REST>
            if (!ComponentAccessor.getJiraAuthenticationContext().isLoggedInUser()) {

                /* The user is not logged in. */

                // Capture the value of the devOpticsReturnUrl query param so we know where to redirect back to.
                final String devOpticsReturnUrl = req.getParameter("devOpticsReturnUrl");
                if (StringUtils.isNotBlank(devOpticsReturnUrl)) {
                    req.getSession().setAttribute("devOpticsReturnUrl", devOpticsReturnUrl);
                } else {
                    // Didn't get a devOpticsReturnUrl query param.  Bad request.
                    resp.getWriter().println("Missing devOpticsReturnUrl query parameter");
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    return;
                }

                // Redirect to Jira's login, requesting to come back here again on successful login.
                final String returnUrl = req.getRequestURI();
                resp.sendRedirect("/login.jsp?os_destination=" + URLEncoder.encode(returnUrl, "UTF-8"));
                return;
            }

            /* If we get to here, the user is now logged in. */

            // Redirect to wherever we were told to go in the devOpticsReturnUrl query param from the initial request.
            final String devOpticsReturnUrl = (String) req.getSession().getAttribute("devOpticsReturnUrl");
            if (devOpticsReturnUrl != null) {
                resp.sendRedirect(devOpticsReturnUrl);
                return;
            }
        }

        // If all else fails, say Not Found.
        resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
    }
}