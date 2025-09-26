package com.dabsquared.gitlabjenkins.connection;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import hudson.model.FreeStyleProject;
import hudson.model.Item;
import hudson.security.GlobalMatrixAuthorizationStrategy;
import hudson.util.FormValidation;
import hudson.util.Secret;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.client.server.MockServerClient;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Robin Müller
 */
public class GitLabConnectionConfigTest {

    private static final String API_TOKEN = "secret";
    private static final String API_TOKEN_ID = "apiTokenId";

    @Rule
    public MockServerRule mockServer = new MockServerRule(this);
    @Rule
    public JenkinsRule jenkins = new JenkinsRule();
    private MockServerClient mockServerClient;
    private String gitLabUrl;

    @Before
    public void setup() throws IOException {
        gitLabUrl = "http://localhost:" + mockServer.getPort() + "/gitlab";
        for (CredentialsStore credentialsStore : CredentialsProvider.lookupStores(Jenkins.getInstance())) {
            if (credentialsStore instanceof SystemCredentialsProvider.StoreImpl) {
                List<Domain> domains = credentialsStore.getDomains();
                credentialsStore.addCredentials(domains.get(0),
                    new StringCredentialsImpl(CredentialsScope.SYSTEM, API_TOKEN_ID, "GitLab API Token", Secret.fromString(API_TOKEN)));
            }
        }
    }

    @Test
    public void doCheckConnection_success() {
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.OK.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, false, 10, 10);

        assertThat(formValidation.getMessage(), is(Messages.connection_success()));
        mockServerClient.verify(request);
    }

    @Test
    public void doCheckConnection_forbidden() throws IOException {
        HttpRequest request = request().withPath("/gitlab/api/v3/.*").withHeader("PRIVATE-TOKEN", API_TOKEN);
        mockServerClient.when(request).respond(response().withStatusCode(Response.Status.FORBIDDEN.getStatusCode()));

        GitLabConnectionConfig connectionConfig = jenkins.get(GitLabConnectionConfig.class);
        FormValidation formValidation = connectionConfig.doTestConnection(gitLabUrl, API_TOKEN_ID, false, 10, 10);

        assertThat(formValidation.getMessage(), is(Messages.connection_error("HTTP 403 Forbidden")));
        mockServerClient.verify(request);
    }

    @Test
    public void authenticationEnabled_anonymous_forbidden() throws IOException, URISyntaxException {
        Boolean defaultValue = jenkins.get(GitLabConnectionConfig.class).getUseAuthenticatedEndpoint();
        assertTrue(defaultValue);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        FreeStyleProject project = jenkins.createFreeStyleProject("test");
        GitLabPushTrigger trigger = mock(GitLabPushTrigger.class);
        project.addTrigger(trigger);

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(403));
    }

    @Test
    public void authenticationEnabled_registered_success() throws Exception {
        String username = "test-user";
        jenkins.getInstance().setSecurityRealm(jenkins.createDummySecurityRealm());
        GlobalMatrixAuthorizationStrategy authorizationStrategy = new GlobalMatrixAuthorizationStrategy();
        authorizationStrategy.add(Item.BUILD, username);
        jenkins.getInstance().setAuthorizationStrategy(authorizationStrategy);
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        String auth = username + ":" + username;
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + new String(Base64.encodeBase64(auth.getBytes(Charset.forName("ISO-8859-1")))));
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }

    @Test
    public void authenticationDisabled_anonymous_success() throws IOException, URISyntaxException {
        jenkins.get(GitLabConnectionConfig.class).setUseAuthenticatedEndpoint(false);
        jenkins.getInstance().setAuthorizationStrategy(new GlobalMatrixAuthorizationStrategy());
        URL jenkinsURL = jenkins.getURL();
        jenkins.createFreeStyleProject("test");

        CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(jenkinsURL.toExternalForm() + "project/test");
        request.addHeader("X-Gitlab-Event", "Push Hook");
        request.setEntity(new StringEntity("{}"));

        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(200));
    }
}
