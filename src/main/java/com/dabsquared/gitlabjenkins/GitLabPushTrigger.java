package com.dabsquared.gitlabjenkins;

import hudson.Extension;
import hudson.Util;
import hudson.console.AnnotatedLargeText;
import hudson.model.*;
import hudson.plugins.git.*;
import hudson.triggers.SCMTrigger;
import hudson.triggers.SCMTrigger.SCMTriggerCause;

import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.SequentialExecutionQueue;
import hudson.util.StreamTaskListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;
import jenkins.model.JenkinsLocationConfiguration;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.ObjectId;
import org.kohsuke.stapler.DataBoundConstructor;

import jenkins.model.Jenkins.MasterComputer;

import org.apache.commons.jelly.XMLOutput;

import com.dabsquared.gitlabjenkins.GitLabPushRequest.Commit;

/**
 * Triggers a build when we receive a GitLab WebHook.
 *
 * @author Daniel Brooks
 */
public class GitLabPushTrigger extends Trigger<AbstractProject<?, ?>> {
    private boolean triggerOnPush;
    private boolean triggerOnMergeRequest;

    @DataBoundConstructor
    public GitLabPushTrigger(boolean triggerOnPush, boolean triggerOnMergeRequest) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnMergeRequest = triggerOnMergeRequest;
    }
    
    public boolean getTriggerOnPush() {
    	return triggerOnPush;
    }
    
    public boolean getTriggerOnMergeRequest() {
    	return triggerOnMergeRequest;
    }

    public void onPost(final GitLabPushRequest req) {
        if (triggerOnPush) {
            getDescriptor().queue.execute(new Runnable() {

                public void run() {
                    LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                    String name = " #" + job.getNextBuildNumber();
                    GitLabPushCause cause = createGitLabPushCause(req);
                    Action[] actions = createActions(req);

                    if (job.scheduleBuild(job.getQuietPeriod(), cause, actions)) {
                        LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                    } else {
                        LOGGER.log(Level.INFO, "GitLab Push Request detected in {0}. Job is already in the queue.", job.getName());
                    }
                }

                private GitLabPushCause createGitLabPushCause(GitLabPushRequest req) {
                    GitLabPushCause cause;
                    String triggeredByUser = req.getCommits().get(0).getAuthor().getName();
                    try {
                        cause = new GitLabPushCause(triggeredByUser, getLogFile());
                    } catch (IOException ex) {
                        cause = new GitLabPushCause(triggeredByUser);
                    }
                    return cause;
                }

                private Action[] createActions(GitLabPushRequest req) {
                    ArrayList<Action> actions = new ArrayList<Action>();

                    String branch = req.getRef().replaceAll("refs/heads/", "");

                    LOGGER.log(Level.INFO, "GitLab Push Request from branch {0}.", branch);

                    Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
                    values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", branch));
                    values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", branch));
                    values.put("gitlabBranch", new StringParameterValue("gitlabBranch", branch));

                    List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

                    ParametersAction parametersAction = new ParametersAction(listValues);
                    actions.add(parametersAction);

                    RevisionParameterAction revision = new RevisionParameterAction(req.getLastCommit().getId());
                    actions.add(revision);

                    Action[] actionsArray = actions.toArray(new Action[0]);

                    return actionsArray;
                }

            });
        }
    }

    public void onPost(final GitLabMergeRequest req) {
        if (triggerOnMergeRequest) {
            getDescriptor().queue.execute(new Runnable() {
                public void run() {
                    LOGGER.log(Level.INFO, "{0} triggered.", job.getName());
                    String name = " #" + job.getNextBuildNumber();
                    GitLabMergeCause cause = createGitLabMergeCause(req);
                    Action[] actions = createActions(req);

                    if (job.scheduleBuild(job.getQuietPeriod(), cause, actions)) {
                        LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Triggering {1}", new String[]{job.getName(), name});
                    } else {
                        LOGGER.log(Level.INFO, "GitLab Merge Request detected in {0}. Job is already in the queue.", job.getName());
                    }
                }

                private GitLabMergeCause createGitLabMergeCause(GitLabMergeRequest req) {
                    GitLabMergeCause cause;
                    try {
                        cause = new GitLabMergeCause(req, getLogFile());
                    } catch (IOException ex) {
                        cause = new GitLabMergeCause(req);
                    }
                    return cause;
                }

                private Action[] createActions(GitLabMergeRequest req) {
                    List<Action> actions = new ArrayList<Action>();

                    Map<String, ParameterValue> values = new HashMap<String, ParameterValue>();
                    values.put("gitlabSourceBranch", new StringParameterValue("gitlabSourceBranch", String.valueOf(req.getObjectAttribute().getSourceBranch())));
                    values.put("gitlabTargetBranch", new StringParameterValue("gitlabTargetBranch", String.valueOf(req.getObjectAttribute().getTargetBranch())));

                    List<ParameterValue> listValues = new ArrayList<ParameterValue>(values.values());

                    ParametersAction parametersAction = new ParametersAction(listValues);
                    actions.add(parametersAction);

                    Action[] actionsArray = actions.toArray(new Action[0]);

                    return actionsArray;
                }

            });
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DescriptorImpl.get();
    }

    public File getLogFile() {
        return new File(job.getRootDir(), "gitlab-polling.log");
    }

    private static final Logger LOGGER = Logger.getLogger(GitLabPushTrigger.class.getName());



    @Extension
    public static class DescriptorImpl extends TriggerDescriptor {

        AbstractProject project;

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);

        @Override
        public boolean isApplicable(Item item) {
            if(item instanceof AbstractProject) {
                project = (AbstractProject) item;
                return true;
            } else {
                return false;
            }
        }

        @Override
        public String getDisplayName() {
            if(project == null) {
                return "Build when a change is pushed to GitLab, unknown URL";
            }

            final List<String> projectParentsUrl = new ArrayList<String>();
            for (Object parent = project.getParent(); parent instanceof Item; parent = ((Item) parent).getParent()) {
                projectParentsUrl.add(0, ((Item) parent).getName());
            }

            final StringBuilder projectUrl = new StringBuilder();
            projectUrl.append(Jenkins.getInstance().getRootUrl());
            projectUrl.append(GitLabWebHook.WEBHOOK_URL);
            projectUrl.append('/');
            for (final String parentUrl : projectParentsUrl) {
                projectUrl.append(Util.rawEncode(parentUrl));
                projectUrl.append('/');
            }
            projectUrl.append(Util.rawEncode(project.getName()));

            return "Build when a change is pushed to GitLab. GitLab CI Service URL: " + projectUrl;
        }

        @Override
        public String getHelpFile() {
            return "/plugin/gitlab-jenkins/help/help-trigger.jelly";
        }

        public static DescriptorImpl get() {
            return Trigger.all().get(DescriptorImpl.class);
        }

    }
}
