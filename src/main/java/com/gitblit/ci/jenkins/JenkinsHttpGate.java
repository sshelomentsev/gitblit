package com.gitblit.ci.jenkins;

import com.gitblit.ci.jenkins.model.BuildInfo;
import com.gitblit.ci.jenkins.model.CheckJobResult;
import com.gitblit.models.TicketModel;
import com.gitblit.utils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class provides methods to work with Jenkins via HTTP.
 *
 * @author Yaroslav Pankratyev
 */
public final class JenkinsHttpGate implements AutoCloseable
{
    private static final String ENDPOINT_CHECK_JOB = "/job/{0}/api/json";
    private static final String ENDPOINT_BUILD_STATUSES = "/gitblit/buildStatuses";
    private static final String JSON_KEY_BUILD_URL = "url";
    private static final String JSON_KEY_BUILD_RESULT = "result";

    private final CloseableHttpClient client;
    private final HttpContext localContext;
    private final String jobName;
    private final String jenkinsHost;

    /**
     * Creates Jenkins HTTP gate with given configuration.
     * @param host Jenkins root URL.
     * @param username Jenkins username; might be null or empty.
     * @param apiToken Jenkins API token for given username.
     * @param jobName Jenkins job name.
     * @throws NullPointerException if given host is null.
     * @throws IllegalArgumentException if given host is not a valid URL.
     */
    public JenkinsHttpGate(String host, String username, String apiToken, String jobName) {
        URI uri = URI.create(host);
        HttpClientBuilder clientBuilder = HttpClients.custom();
        HttpClientContext localContext = null;

        // basic authentication
        if (!StringUtils.isEmpty(username)) {
            CredentialsProvider credentialsProvider = new BasicCredentialsProvider();

            String scheme = uri.getScheme();
            if (scheme == null) {
                scheme = "http";
            }

            int port = uri.getPort();
            if (port < 0) {
                if ("http".equalsIgnoreCase(scheme)) {
                    port = 80;
                } else if ("https".equalsIgnoreCase(scheme)) {
                    port = 443;
                }
            }

            credentialsProvider.setCredentials(new AuthScope(uri.getHost(), port),
                                               new UsernamePasswordCredentials(username, apiToken));
            clientBuilder.setDefaultCredentialsProvider(credentialsProvider);

            AuthCache authCache = new BasicAuthCache();
            BasicScheme basicAuth = new BasicScheme();
            authCache.put(new HttpHost(uri.getHost(), port, scheme), basicAuth);
            localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);
        }
        client = clientBuilder.build();
        this.localContext = localContext;
        this.jobName = fixJobName(jobName);
        if (host.endsWith("/")) {
            jenkinsHost = host.substring(0, host.length() - 1);
        } else {
            jenkinsHost = host;
        }
    }

    private String fixJobName(String jobName) {
        if (jobName.endsWith("/")) {
            jobName = jobName.substring(0, jobName.length() - 1);
        }
        if (jobName.startsWith("/")) {
            jobName = jobName.substring(1, jobName.length());
        }
        return jobName.replace("/", "/job/"); // support for Folders plugin
    }

    public List<BuildInfo> getCommitBuildStatuses(List<String> commits)
            throws JenkinsException {
        String query = jenkinsHost + ENDPOINT_BUILD_STATUSES + "?commits=" + join(",", commits) + "&job=" + jobName;
        HttpGet method = new HttpGet(query);

        CloseableHttpResponse response = null;
        try {
            response = client.execute(method, localContext);
            int httpCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == httpCode) {
                try {
                    JSONObject responseJson = new JSONObject(EntityUtils.toString(response.getEntity()));
                    List<BuildInfo> buildInfos = new ArrayList<>();
                    Iterator<String> keysIter = responseJson.keys();
                    while (keysIter.hasNext()) {
                        String sha1 = keysIter.next();
                        JSONObject infoForSha1 = responseJson.getJSONObject(sha1);
                        String buildUrl = infoForSha1.getString(JSON_KEY_BUILD_URL);
                        String buildResult = infoForSha1.getString(JSON_KEY_BUILD_RESULT);
                        try {
                            TicketModel.CIScore ciScore = TicketModel.CIScore.fromJenkinsBuildResult(buildResult);
                            buildInfos.add(new BuildInfo(sha1, ciScore, buildUrl));
                        } catch (NoSuchElementException ignore) {
                        }
                    }
                    return buildInfos;
                } catch (JSONException e) {
                    throw new JenkinsException("Cannot process response JSON", e);
                }
            } else if (HttpStatus.SC_FORBIDDEN == httpCode) {
                throw new JenkinsException("Forbidden");
            } else if (HttpStatus.SC_BAD_REQUEST == httpCode) {
                throw new JenkinsException(String.format(
                        "Bad request; job: %s, commits: %s", jobName, commits.toString()));
            } else if (HttpStatus.SC_INTERNAL_SERVER_ERROR == httpCode) {
                throw new JenkinsException("Internal Jenkins server error");
            } else if (HttpStatus.SC_NOT_FOUND == httpCode) {
                throw new JenkinsException("Not found; job: " + jobName);
            } else {
                throw new JenkinsException("Unknown HTTP status code in the response: " + httpCode);
            }
        } catch (IOException e) {
            throw new JenkinsException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    private static String join(String delimiter, List<String> parts) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.size() - 1; i++) {
            builder.append(parts.get(i)).append(delimiter);
        }
        if(parts.size() > 0){
            builder.append(parts.get(parts.size() - 1));
        }
        return builder.toString();
    }

    public CheckJobResult checkJob() throws JenkinsException {
        String query = jenkinsHost + MessageFormat.format(ENDPOINT_CHECK_JOB, jobName);
        HttpGet method = new HttpGet(query);

        CloseableHttpResponse response = null;
        try {
            response = client.execute(method, localContext);
            int httpCode = response.getStatusLine().getStatusCode();
            if (HttpStatus.SC_OK == httpCode) {
                return CheckJobResult.Ok;
            } else if (HttpStatus.SC_FORBIDDEN == httpCode || HttpStatus.SC_UNAUTHORIZED == httpCode) {
                return CheckJobResult.Forbidden;
            } else if (HttpStatus.SC_NOT_FOUND == httpCode) {
                return CheckJobResult.NotFound;
            } else {
                throw new JenkinsException("Unknown HTTP status code in the response: " + httpCode);
            }
        } catch (IOException e) {
            throw new JenkinsException(e);
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(client);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }
}
