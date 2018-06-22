### Pre-reqs
* Have a local JIRA server instance running on port 8080
  * Download from [here](https://www.atlassian.com/software/jira/download).
  * Unofficial JIRA server in a Docker image [here](https://hub.docker.com/r/cptactionhank/atlassian-jira/).

### Pages of interest
* The served HTML page can be found in `/src/main/resources/serve-me.html`
* To speed up development, the plugin does not load the `serve-me.html` page from the classpath, but directly from the filesystem at `/Users/paulwoolley/code/devoptics-jira-plugin/src/main/resources/serve-me.html`. This is hardcoded in to `DevOpticsJIRAServlet.java`.  Obviously, if you are not me, then you will need to change this hardcoded path, rebuild and reinstall the plugin.
* The plugin's servlet code is `/src/main/java/com/cloudbees/devoptics/jira/DevOpticsJIRAServlet.java`

### Installation in to Jira server
* Do a `mvn package` to build the plugin classes in to a `.jar` file.
* Login to Jira Server as admin
* Under Jira Administration, go to Add-ons -> Manage add-ons -> Upload add-on, and select the plugin `.jar` file.

### API
* Base is `http://localhost:8080/plugins/servlet/devoptics`
* Html and js for iframe: `/load.html`
* Check to see if a user is logged in: `/is-logged-in`
* Login with redirect: `/do-login?devOpticsReturnUrl={urlencoded url to return to after login}`.
