package com.dabsquared.gitlabjenkins.webhook.build;

import java.util.logging.Logger;

import hudson.model.Item;
import hudson.model.Job;
import hudson.security.Messages;
import hudson.security.Permission;
import hudson.util.HttpResponses;
import jenkins.model.Jenkins;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerResponse;
import com.dabsquared.gitlabjenkins.GitLabPushTrigger;
import com.dabsquared.gitlabjenkins.connection.GitLabConnectionConfig;
import com.dabsquared.gitlabjenkins.webhook.WebHookAction;

/**
 * @author Xinran Xiao
 */
abstract class BuildWebHookAction implements WebHookAction {

    private final static Logger LOGGER = Logger.getLogger(BuildWebHookAction.class.getName());

    abstract void processForCompatibility();

    abstract void execute();

    public final void execute(StaplerResponse response) {
        processForCompatibility();
        execute();
    }

    protected abstract static class TriggerNotifier implements Runnable {

        private final Item project;
        private final String secretToken;
        private final Authentication authentication;

        public TriggerNotifier(Item project, String secretToken, Authentication authentication) {
            this.project = project;
            this.secretToken = secretToken;
            this.authentication = authentication;
        }

        public void run() {
            GitLabPushTrigger trigger = GitLabPushTrigger.getFromJob((Job<?, ?>) project);
            if (trigger != null) {
                if (StringUtils.isEmpty(trigger.getSecretToken())) {
                    checkPermission(Item.BUILD);
                } else if (!StringUtils.equals(trigger.getSecretToken(), secretToken)) {
                    throw HttpResponses.errorWithoutStack(401, "Invalid token");
                }
                performOnPost(trigger);
            }
        }

        private void checkPermission(Permission permission) {
            if (((GitLabConnectionConfig) Jenkins.getInstance().getDescriptor(GitLabConnectionConfig.class)).getUseAuthenticatedEndpoint()) {
                if (!Jenkins.getActiveInstance().getACL().hasPermission(authentication, permission)) {
                    String message = Messages.AccessDeniedException2_MissingPermission(authentication.getName(), permission.group.title+"/"+permission.name);
                    LOGGER.finest("Unauthorized (Did you forget to add API Token to the web hook ?)");
                    throw HttpResponses.errorWithoutStack(403, message);
                }
            }
        }

        protected abstract void performOnPost(GitLabPushTrigger trigger);
    }
}
