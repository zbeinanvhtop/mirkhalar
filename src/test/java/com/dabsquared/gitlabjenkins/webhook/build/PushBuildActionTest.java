package com.dabsquared.gitlabjenkins.webhook.build;

import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.model.PushHook;
import hudson.model.FreeStyleProject;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.HttpResponses;
import org.kohsuke.stapler.StaplerResponse;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Robin Müller
 */
@RunWith(MockitoJUnitRunner.class)
public class PushBuildActionTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private StaplerResponse response;

    @Mock
    private GitLabPushTrigger trigger;

    @Test
    public void skip_missingRepositoryUrl() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject("test");
        testProject.addTrigger(trigger);

        new PushBuildAction(testProject, getJson("PushEvent_missingRepositoryUrl.json")).execute(response);

        verify(trigger, never()).onPost(any(PushHook.class));
    }

    @Test
    public void build() throws IOException {
        FreeStyleProject testProject = jenkins.createFreeStyleProject("test");
        when(trigger.getTriggerOpenMergeRequestOnPush()).thenReturn("never");
        testProject.addTrigger(trigger);

        exception.expect(HttpResponses.HttpResponseException.class);
        new PushBuildAction(testProject, getJson("PushEvent.json")).execute(response);

        verify(trigger).onPost(any(PushHook.class));
    }

    private String getJson(String name) throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream(name));
    }
}
