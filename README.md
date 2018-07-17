### Pre-reqs
* Have a local JIRA server instance running on port 8080.  Choices are:
  * Download a JIRA server install from [here](https://www.atlassian.com/software/jira/download), or
  * Use the unofficial JIRA server in a Docker image [here](https://hub.docker.com/r/cptactionhank/atlassian-jira/).

### Files of interest
* The served HTML page can be found in `/src/main/resources/serve-me.html`
* The plugin's servlet code is `/src/main/java/com/cloudbees/devoptics/jira/DevOpticsJIRAServerPluginServlet.java`
* Servlet unit test is `/src/test/java/com/cloudbees/devoptics/jira/DevOpticsJIRAServerPluginServletTest.java`

### Installation in to Jira server
* Do a `mvn clean package` to build the plugin classes in to a `.jar` file.
* Login to Jira Server as admin
* Under Jira Administration, go to Add-ons -> Manage add-ons -> Upload add-on, and select the plugin `.jar` file.

### API
* Base is `http://localhost:8080/plugins/servlet/devoptics`
* Html and js for iframe: `/load.html`
* Check to see if a user is logged in: `/is-logged-in`
* Login with redirect: `/do-login?devOpticsReturnUrl={urlencoded url to return to after login}`.

### Gotcha for the future me
At one point, to speed up the development cycle, I would have the HTML page loaded from an absolute path on the filesystem, instead of from the classpath.  **Remember**, if you're using a JIRA Server in a docker container for development, the plugin will look to load the HTML from the docker container's filesystem, not your host machine's filesystem.  To resolve this, launch the docker container with a host_dir to container_dir mapping using `-v host_dir:container_dir`. E.g.
```bash
# In the plugin code, load the HTML from /home and it will be found in src/main/resources
docker run -v /Users/paulwoolley/code/devoptics-jira-plugin/src/main/resources:/home --detach --publish 8080:8080 cptactionhank/atlassian-jira:latest
```
