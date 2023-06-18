package com.dabsquared.gitlabjenkins.trigger.handler.merge;

import com.dabsquared.gitlabjenkins.model.MergeRequestHook;
import com.dabsquared.gitlabjenkins.trigger.handler.WebHookTriggerConfig;
import hudson.model.Job;

/**
 * @author Robin Müller
 */
class NopMergeRequestHookTriggerHandler implements MergeRequestHookTriggerHandler {
    @Override
    public void handle(WebHookTriggerConfig config, Job<?, ?> job, MergeRequestHook hook) {
        // nothing to do
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
