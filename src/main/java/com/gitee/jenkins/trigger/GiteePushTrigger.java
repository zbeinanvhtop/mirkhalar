package com.gitee.jenkins.trigger;


import com.gitee.jenkins.connection.GiteeConnection;
import com.gitee.jenkins.connection.GiteeConnectionConfig;
import com.gitee.jenkins.connection.GiteeConnectionProperty;
import com.gitee.jenkins.gitee.hook.model.MergeRequestHook;
import com.gitee.jenkins.gitee.hook.model.NoteHook;
import com.gitee.jenkins.gitee.hook.model.PipelineHook;
import com.gitee.jenkins.gitee.hook.model.PushHook;
import com.gitee.jenkins.publisher.GiteeAcceptMergeRequestPublisher;
import com.gitee.jenkins.publisher.GiteeMessagePublisher;
import com.gitee.jenkins.trigger.filter.BranchFilter;
import com.gitee.jenkins.trigger.filter.BranchFilterFactory;
import com.gitee.jenkins.trigger.filter.BranchFilterType;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilter;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilterConfig;
import com.gitee.jenkins.trigger.filter.MergeRequestLabelFilterFactory;
import com.gitee.jenkins.trigger.handler.merge.MergeRequestHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.note.NoteHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.pipeline.PipelineHookTriggerHandler;
import com.gitee.jenkins.trigger.handler.push.PushHookTriggerHandler;
import com.gitee.jenkins.webhook.GiteeWebHook;
import hudson.Extension;
import hudson.Util;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.model.AbstractProject;
import hudson.model.Item;
import hudson.model.Job;
import hudson.triggers.Trigger;
import hudson.triggers.TriggerDescriptor;
import hudson.util.Secret;
import hudson.util.SequentialExecutionQueue;
import jenkins.model.Jenkins;
import jenkins.model.ParameterizedJobMixIn;
import jenkins.triggers.SCMTriggerItem.SCMTriggerItems;
import net.karneim.pojobuilder.GeneratePojoBuilder;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.Ancestor;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.Stapler;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.security.SecureRandom;

import static com.gitee.jenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;
import static com.gitee.jenkins.trigger.handler.merge.MergeRequestHookTriggerHandlerFactory.newMergeRequestHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.note.NoteHookTriggerHandlerFactory.newNoteHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.pipeline.PipelineHookTriggerHandlerFactory.newPipelineHookTriggerHandler;
import static com.gitee.jenkins.trigger.handler.push.PushHookTriggerHandlerFactory.newPushHookTriggerHandler;


/**
 * Triggers a build when we receive a Gitee WebHook.
 *
 * @author Daniel Brooks
 * @author Yashin Luo
 *
 */
public class GiteePushTrigger extends Trigger<Job<?, ?>> {

    private static final SecureRandom RANDOM = new SecureRandom();

    private boolean triggerOnPush = true;
    private boolean triggerOnOpenMergeRequest = true;
    private boolean triggerOnPipelineEvent = false;
    private boolean triggerOnAcceptedMergeRequest = false;
    private boolean triggerOnUpdateMergeRequest = false;
    private boolean triggerOnClosedMergeRequest = false;
    private boolean triggerOnApprovedMergeRequest = false;
    private boolean triggerOnTestedMergeRequest = false;
    private boolean triggerOnNoteRequest = true;
    private String noteRegex = "";
    private boolean ciSkip = true;
    private boolean skipWorkInProgressMergeRequest;
    private boolean setBuildDescription = true;
    private transient boolean addNoteOnMergeRequest;
    private transient boolean addCiMessage;
    private transient boolean addVoteOnMergeRequest;
    private transient boolean allowAllBranches = false;
    private transient String branchFilterName;
    private BranchFilterType branchFilterType;
    private String includeBranchesSpec;
    private String excludeBranchesSpec;
    private String targetBranchRegex;
    private MergeRequestLabelFilterConfig mergeRequestLabelFilterConfig;
    private volatile Secret secretToken;
    private String pendingBuildName;
    private boolean cancelPendingBuildsOnUpdate;

    private transient BranchFilter branchFilter;
    private transient PushHookTriggerHandler pushHookTriggerHandler;
    private transient MergeRequestHookTriggerHandler mergeRequestHookTriggerHandler;
    private transient NoteHookTriggerHandler noteHookTriggerHandler;
    private transient PipelineHookTriggerHandler pipelineTriggerHandler;
    private transient boolean acceptMergeRequestOnSuccess;
    private transient MergeRequestLabelFilter mergeRequestLabelFilter;

    /**
     * @deprecated use {@link #GiteePushTrigger()} with setters to configure an instance of this class.
     */
    @Deprecated
    @GeneratePojoBuilder(intoPackage = "*.builder.generated", withFactoryMethod = "*")
    public GiteePushTrigger(boolean triggerOnPush, boolean triggerOnOpenMergeRequest, boolean triggerOnUpdateMergeRequest, boolean triggerOnAcceptedMergeRequest, boolean triggerOnClosedMergeRequest,
                            boolean triggerOnNoteRequest, String noteRegex,
                            boolean skipWorkInProgressMergeRequest, boolean ciSkip,
                            boolean setBuildDescription, boolean addNoteOnMergeRequest, boolean addCiMessage, boolean addVoteOnMergeRequest,
                            boolean acceptMergeRequestOnSuccess, BranchFilterType branchFilterType,
                            String includeBranchesSpec, String excludeBranchesSpec, String targetBranchRegex,
                            MergeRequestLabelFilterConfig mergeRequestLabelFilterConfig, String secretToken, boolean triggerOnPipelineEvent,
                            boolean triggerOnApprovedMergeRequest, String pendingBuildName, boolean cancelPendingBuildsOnUpdate) {
        this.triggerOnPush = triggerOnPush;
        this.triggerOnOpenMergeRequest = triggerOnOpenMergeRequest;
        this.triggerOnUpdateMergeRequest = triggerOnUpdateMergeRequest;
        this.triggerOnAcceptedMergeRequest = triggerOnAcceptedMergeRequest;
        this.triggerOnClosedMergeRequest = triggerOnClosedMergeRequest;
        this.triggerOnNoteRequest = triggerOnNoteRequest;
        this.noteRegex = noteRegex;
        this.triggerOnPipelineEvent = triggerOnPipelineEvent;
        this.ciSkip = ciSkip;
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
        this.setBuildDescription = setBuildDescription;
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
        this.addCiMessage = addCiMessage;
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
        this.branchFilterType = branchFilterType;
        this.includeBranchesSpec = includeBranchesSpec;
        this.excludeBranchesSpec = excludeBranchesSpec;
        this.targetBranchRegex = targetBranchRegex;
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;
        this.mergeRequestLabelFilterConfig = mergeRequestLabelFilterConfig;
        this.secretToken = Secret.fromString(secretToken);
        this.triggerOnApprovedMergeRequest = triggerOnApprovedMergeRequest;
        this.pendingBuildName = pendingBuildName;
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;

        initializeTriggerHandler();
        initializeBranchFilter();
        initializeMergeRequestLabelFilter();
    }

    @DataBoundConstructor
    public GiteePushTrigger() { }

    @Initializer(after = InitMilestone.JOB_LOADED)
    public static void migrateJobs() throws IOException {
        GiteePushTrigger.DescriptorImpl oldConfig = Trigger.all().get(GiteePushTrigger.DescriptorImpl.class);
        if (!oldConfig.jobsMigrated) {
            GiteeConnectionConfig giteeConfig = (GiteeConnectionConfig) Jenkins.getInstance().getDescriptor(GiteeConnectionConfig.class);
            giteeConfig.getConnections().add(new GiteeConnection(
                oldConfig.giteeHostUrl,
                    oldConfig.giteeHostUrl,
                    oldConfig.GiteeApiToken,
                "autodetect",
                    oldConfig.ignoreCertificateErrors,
                    10,
                    10));

            String defaultConnectionName = giteeConfig.getConnections().get(0).getName();
            for (AbstractProject<?, ?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GiteePushTrigger trigger = project.getTrigger(GiteePushTrigger.class);
                if (trigger != null) {
                    project.addProperty(new GiteeConnectionProperty(defaultConnectionName));
                    project.save();
                }
            }
            giteeConfig.save();
            oldConfig.jobsMigrated = true;
            oldConfig.save();
        }
        if (!oldConfig.jobsMigrated2) {
            for (AbstractProject<?, ?> project : Jenkins.getInstance().getAllItems(AbstractProject.class)) {
                GiteePushTrigger trigger = project.getTrigger(GiteePushTrigger.class);
                if (trigger != null) {
                    if (trigger.addNoteOnMergeRequest) {
                        project.getPublishersList().add(new GiteeMessagePublisher());
                    }
                    if (trigger.acceptMergeRequestOnSuccess) {
                        project.getPublishersList().add(new GiteeAcceptMergeRequestPublisher());
                    }
                    project.save();
                }
            }
            oldConfig.jobsMigrated2 = true;
            oldConfig.save();
        }
    }

    public boolean getTriggerOnPush() {
        return triggerOnPush;
    }

    public boolean getTriggerOnOpenMergeRequest() {
        return triggerOnOpenMergeRequest;
    }

    public boolean getTriggerOnTestedMergeRequest() {
        return triggerOnTestedMergeRequest;
    }

    public boolean getTriggerOnUpdateMergeRequest() {
        return triggerOnUpdateMergeRequest;
    }

    public boolean isTriggerOnAcceptedMergeRequest() {
        return triggerOnAcceptedMergeRequest;
    }

    public boolean isTriggerOnApprovedMergeRequest() {
		return triggerOnApprovedMergeRequest;
	}    
    
    public boolean isTriggerOnClosedMergeRequest() {
        return triggerOnClosedMergeRequest;
    }

    public boolean getTriggerOnNoteRequest() {
        return triggerOnNoteRequest;
    }

    public boolean getTriggerOnPipelineEvent() { return triggerOnPipelineEvent; }

    public String getNoteRegex() {
        return this.noteRegex == null ? "" : this.noteRegex;
    }

    public boolean getSetBuildDescription() {
        return setBuildDescription;
    }

    public boolean getCiSkip() {
        return ciSkip;
    }

    public boolean isSkipWorkInProgressMergeRequest() {
        return skipWorkInProgressMergeRequest;
    }

    public BranchFilterType getBranchFilterType() {
        return branchFilterType;
    }

    public String getIncludeBranchesSpec() {
        return includeBranchesSpec;
    }

    public String getExcludeBranchesSpec() {
        return excludeBranchesSpec;
    }

    public String getTargetBranchRegex() {
        return targetBranchRegex;
    }

    public MergeRequestLabelFilterConfig getMergeRequestLabelFilterConfig() {
        return mergeRequestLabelFilterConfig;
    }

    public String getSecretToken() {
        return secretToken == null ? null : secretToken.getPlainText();
    }

    public String getPendingBuildName() {
        return pendingBuildName;
    }

    public boolean getCancelPendingBuildsOnUpdate() {
        return this.cancelPendingBuildsOnUpdate;
    }

    @DataBoundSetter
    public void setTriggerOnPush(boolean triggerOnPush) {
        this.triggerOnPush = triggerOnPush;
    }
    
    @DataBoundSetter
    public void setTriggerOnApprovedMergeRequest(boolean triggerOnApprovedMergeRequest) {
        this.triggerOnApprovedMergeRequest = triggerOnApprovedMergeRequest;
    }

    @DataBoundSetter
    public void setTriggerOnTestedMergeRequest(boolean triggerOnTestedMergeRequest) {
        this.triggerOnTestedMergeRequest = triggerOnTestedMergeRequest;
    }

    @DataBoundSetter
    public void setTriggerOnOpenMergeRequest(boolean triggerOnOpenMergeRequest) {
        this.triggerOnOpenMergeRequest = triggerOnOpenMergeRequest;
    }

    @DataBoundSetter
    public void setTriggerOnAcceptedMergeRequest(boolean triggerOnAcceptedMergeRequest) {
        this.triggerOnAcceptedMergeRequest = triggerOnAcceptedMergeRequest;
    }

    @DataBoundSetter
    public void setTriggerOnClosedMergeRequest(boolean triggerOnClosedMergeRequest) {
        this.triggerOnClosedMergeRequest = triggerOnClosedMergeRequest;
    }

    @DataBoundSetter
    public void setTriggerOnNoteRequest(boolean triggerOnNoteRequest) {
        this.triggerOnNoteRequest = triggerOnNoteRequest;
    }

    @DataBoundSetter
    public void setNoteRegex(String noteRegex) {
        this.noteRegex = noteRegex;
    }

    @DataBoundSetter
    public void setCiSkip(boolean ciSkip) {
        this.ciSkip = ciSkip;
    }

    @DataBoundSetter
    public void setSkipWorkInProgressMergeRequest(boolean skipWorkInProgressMergeRequest) {
        this.skipWorkInProgressMergeRequest = skipWorkInProgressMergeRequest;
    }

    @DataBoundSetter
    public void setSetBuildDescription(boolean setBuildDescription) {
        this.setBuildDescription = setBuildDescription;
    }

    @DataBoundSetter
    public void setAddNoteOnMergeRequest(boolean addNoteOnMergeRequest) {
        this.addNoteOnMergeRequest = addNoteOnMergeRequest;
    }

    @DataBoundSetter
    public void setAddCiMessage(boolean addCiMessage) {
        this.addCiMessage = addCiMessage;
    }

    @DataBoundSetter
    public void setAddVoteOnMergeRequest(boolean addVoteOnMergeRequest) {
        this.addVoteOnMergeRequest = addVoteOnMergeRequest;
    }

    @DataBoundSetter
    public void setBranchFilterName(String branchFilterName) {
        this.branchFilterName = branchFilterName;
    }

    @DataBoundSetter
    public void setBranchFilterType(BranchFilterType branchFilterType) {
        this.branchFilterType = branchFilterType;
    }

    @DataBoundSetter
    public void setIncludeBranchesSpec(String includeBranchesSpec) {
        this.includeBranchesSpec = includeBranchesSpec;
    }

    @DataBoundSetter
    public void setExcludeBranchesSpec(String excludeBranchesSpec) {
        this.excludeBranchesSpec = excludeBranchesSpec;
    }

    @DataBoundSetter
    public void setTargetBranchRegex(String targetBranchRegex) {
        this.targetBranchRegex = targetBranchRegex;
    }

    @DataBoundSetter
    public void setMergeRequestLabelFilterConfig(MergeRequestLabelFilterConfig mergeRequestLabelFilterConfig) {
        this.mergeRequestLabelFilterConfig = mergeRequestLabelFilterConfig;
    }

    @DataBoundSetter
    public void setSecretToken(String secretToken) {
        this.secretToken = Secret.fromString(secretToken);
    }

    @DataBoundSetter
    public void setAcceptMergeRequestOnSuccess(boolean acceptMergeRequestOnSuccess) {
        this.acceptMergeRequestOnSuccess = acceptMergeRequestOnSuccess;
    }

    @DataBoundSetter
    public void setTriggerOnUpdateMergeRequest(boolean triggerOnUpdateMergeRequest) {
        this.triggerOnUpdateMergeRequest = triggerOnUpdateMergeRequest;
    }
    @DataBoundSetter
    public void setTriggerOnPipelineEvent(boolean triggerOnPipelineEvent) {
        this.triggerOnPipelineEvent = triggerOnPipelineEvent;
    }

    @DataBoundSetter
    public void setPendingBuildName(String pendingBuildName) {
        this.pendingBuildName = pendingBuildName;
    }

    @DataBoundSetter
    public void setCancelPendingBuildsOnUpdate(boolean cancelPendingBuildsOnUpdate) {
        this.cancelPendingBuildsOnUpdate = cancelPendingBuildsOnUpdate;
    }

    // executes when the Trigger receives a push request
    public void onPost(final PushHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (mergeRequestLabelFilter == null) {
            initializeMergeRequestLabelFilter();
        }
        if (pushHookTriggerHandler == null) {
            initializeTriggerHandler();
        }
        pushHookTriggerHandler.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
    }

    // executes when the Trigger receives a merge request
    public void onPost(final MergeRequestHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (mergeRequestLabelFilter == null) {
            initializeMergeRequestLabelFilter();
        }
        if (mergeRequestHookTriggerHandler == null) {
            initializeTriggerHandler();
        }
        mergeRequestHookTriggerHandler.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
    }

    // executes when the Trigger receives a note request
    public void onPost(final NoteHook hook) {
        if (branchFilter == null) {
            initializeBranchFilter();
        }
        if (mergeRequestLabelFilter == null) {
            initializeMergeRequestLabelFilter();
        }
        if (noteHookTriggerHandler == null) {
            initializeTriggerHandler();
        }
        noteHookTriggerHandler.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
    }

    // executes when the Trigger receives a pipeline event
    public void onPost(final PipelineHook hook) {
        if (pipelineTriggerHandler == null) {
            initializeTriggerHandler();
        }
        pipelineTriggerHandler.handle(job, hook, ciSkip, branchFilter, mergeRequestLabelFilter);
    }

    private void initializeTriggerHandler() {
		mergeRequestHookTriggerHandler = newMergeRequestHookTriggerHandler(triggerOnOpenMergeRequest,
				triggerOnUpdateMergeRequest, triggerOnAcceptedMergeRequest, triggerOnClosedMergeRequest,
				skipWorkInProgressMergeRequest, triggerOnApprovedMergeRequest, triggerOnTestedMergeRequest, cancelPendingBuildsOnUpdate);
        noteHookTriggerHandler = newNoteHookTriggerHandler(triggerOnNoteRequest, noteRegex);
        pushHookTriggerHandler = newPushHookTriggerHandler(triggerOnPush, skipWorkInProgressMergeRequest);
        pipelineTriggerHandler = newPipelineHookTriggerHandler(triggerOnPipelineEvent);
    }

    private void initializeBranchFilter() {
        if (branchFilterType == null) {
            branchFilterType = BranchFilterType.All;
        }

        branchFilter = BranchFilterFactory.newBranchFilter(branchFilterConfig()
                .withIncludeBranchesSpec(includeBranchesSpec)
                .withExcludeBranchesSpec(excludeBranchesSpec)
                .withTargetBranchRegex(targetBranchRegex)
                .build(branchFilterType));
    }

    private void initializeMergeRequestLabelFilter() {
        mergeRequestLabelFilter = MergeRequestLabelFilterFactory.newMergeRequestLabelFilter(mergeRequestLabelFilterConfig);
    }

    @Override
    protected Object readResolve() throws ObjectStreamException {
        initializeTriggerHandler();
        initializeBranchFilter();
        initializeMergeRequestLabelFilter();
        return super.readResolve();
    }

    public static GiteePushTrigger getFromJob(Job<?, ?> job) {
        GiteePushTrigger trigger = null;
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob) {
            ParameterizedJobMixIn.ParameterizedJob p = (ParameterizedJobMixIn.ParameterizedJob) job;
            for (Trigger t : p.getTriggers().values()) {
                if (t instanceof GiteePushTrigger) {
                    trigger = (GiteePushTrigger) t;
                }
            }
        }
        return trigger;
    }

    @Extension
    @Symbol("gitee")
    public static class DescriptorImpl extends TriggerDescriptor {

        private transient final SequentialExecutionQueue queue = new SequentialExecutionQueue(Jenkins.MasterComputer.threadPoolForRemoting);
        private boolean jobsMigrated = false;
        private boolean jobsMigrated2 = false;
        private String GiteeApiToken;
        private String giteeHostUrl = "";
        private boolean ignoreCertificateErrors = false;

        public DescriptorImpl() {
            load();
        }

        @Override
        public boolean isApplicable(Item item) {
            return item instanceof Job
                    && SCMTriggerItems.asSCMTriggerItem(item) != null
                    && item instanceof ParameterizedJobMixIn.ParameterizedJob;
        }

        @Override
        public String getDisplayName() {
            Job<?, ?> project = retrieveCurrentJob();
            if (project != null) {
                try {
                    return Messages.Build_Gitee_WebHook(retrieveProjectUrl(project));
//                    return "Build when a change is pushed to Gitee";
                } catch (IllegalStateException e) {
                    // nothing to do
                }
            }
            return "Build when a change is pushed to Gitee, unknown URL";
        }

        private StringBuilder retrieveProjectUrl(Job<?, ?> project) {
            return new StringBuilder()
                    .append(Jenkins.getInstance().getRootUrl())
                    .append(GiteeWebHook.WEBHOOK_URL)
                    .append(retrieveParentUrl(project))
                    .append('/').append(Util.rawEncode(project.getName()));
        }

        private StringBuilder retrieveParentUrl(Item item) {
            if (item.getParent() instanceof Item) {
                Item parent = (Item) item.getParent();
                return retrieveParentUrl(parent).append('/').append(Util.rawEncode(parent.getName()));
            } else {
                return new StringBuilder();
            }
        }

        private Job<?, ?> retrieveCurrentJob() {
            StaplerRequest request = Stapler.getCurrentRequest();
            if (request != null) {
                Ancestor ancestor = request.findAncestor(Job.class);
                return ancestor == null ? null : (Job<?, ?>) ancestor.getObject();
            }
            return null;
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }


        public void doGenerateSecretToken(@AncestorInPath final Job<?, ?> project, StaplerResponse response) {
            byte[] random = new byte[16];   // 16x8=128bit worth of randomness, since we use md5 digest as the API token
            RANDOM.nextBytes(random);
            String secretToken = Util.toHexString(random);
            response.setHeader("script", "document.getElementById('secretToken').value='" + secretToken + "'");
        }

        public void doClearSecretToken(@AncestorInPath final Job<?, ?> project, StaplerResponse response) {;
            response.setHeader("script", "document.getElementById('secretToken').value=''");
        }
    }
}
