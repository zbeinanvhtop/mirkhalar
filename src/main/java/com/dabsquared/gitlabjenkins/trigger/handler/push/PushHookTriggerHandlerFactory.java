package com.dabsquared.gitlabjenkins.trigger.handler.push;

import com.dabsquared.gitlabjenkins.trigger.TriggerOpenMergeRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Robin Müller
 */
public final class PushHookTriggerHandlerFactory {

    private PushHookTriggerHandlerFactory() {}

    public static PushHookTriggerHandler newPushHookTriggerHandler(boolean triggerOnPush,
                                                                   TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
                                                                   boolean skipWorkInProgressMergeRequest,
                                                                   boolean cancelPendingBuildsOnUpdate) {
        if (triggerOnPush || triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            return new PushHookTriggerHandlerList(retrieveHandlers(triggerOnPush, triggerOpenMergeRequestOnPush, skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate));
        } else {
            return new NopPushHookTriggerHandler();
        }
    }

    private static List<PushHookTriggerHandler> retrieveHandlers(boolean triggerOnPush,
                                                                 TriggerOpenMergeRequest triggerOpenMergeRequestOnPush,
                                                                 boolean skipWorkInProgressMergeRequest,
                                                                 boolean cancelPendingBuildsOnUpdate) {
        List<PushHookTriggerHandler> result = new ArrayList<>();
        if (triggerOnPush) {
            result.add(new PushHookTriggerHandlerImpl());
        }
        if (triggerOpenMergeRequestOnPush == TriggerOpenMergeRequest.both) {
            result.add(new OpenMergeRequestPushHookTriggerHandler(skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate));
        }
        return result;
    }
}
