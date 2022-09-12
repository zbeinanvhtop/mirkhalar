package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.model.Run;
import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.triggers.Trigger;

import javax.annotation.Nonnull;

/**
 * RunListener that will be called when a build starts and completes.
 * Will lookup GitLabPushTrigger and call onStarted and onCompleted methods
 * in order to have access to the build and set properties.
 */
@Extension
public class GitLabRunListener extends RunListener<Run> {

    @Override
    public void onCompleted(Run run, @Nonnull TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(run);
        if (trig != null) {
            trig.onCompleted(run);
        }
        super.onCompleted(run, listener);
    }

    @Override
    public void onStarted(Run run, TaskListener listener) {
        GitLabPushTrigger trig = getTrigger(run);
        if (trig != null) {
            trig.onStarted(run);
        }
        super.onStarted(run, listener);
    }


    private GitLabPushTrigger getTrigger(Run run) {
        if (run instanceof AbstractBuild) {
            Trigger trig = ((AbstractBuild) run).getProject().getTrigger(GitLabPushTrigger.class);
            if (trig != null && trig instanceof GitLabPushTrigger)
                return (GitLabPushTrigger) trig;
        }
        return null;
    }
}
