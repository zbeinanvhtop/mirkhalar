package com.dabsquared.gitlabjenkins.listener;

import com.dabsquared.gitlabjenkins.trigger.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.cause.GiteeWebHookCause;
import hudson.Extension;
import hudson.model.Cause;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;

import java.io.IOException;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup trigger and call set the build description if necessary.
 *
 * @author Robin Müller
 */
@Extension
public class GitLabBuildDescriptionRunListener extends RunListener<Run<?, ?>> {

    @Override
    public void onStarted(Run<?, ?> build, TaskListener listener) {
        GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob(build.getParent());
        if (trigger != null && trigger.getSetBuildDescription()) {
            Cause cause = build.getCause(GiteeWebHookCause.class);
            if (cause != null && !cause.getShortDescription().isEmpty()) {
                try {
                    build.setDescription(cause.getShortDescription());
                } catch (IOException e) {
                    listener.getLogger().println("Failed to set build description");
                }
            }
        }
    }

}
