package com.gitee.jenkins.trigger.handler.merge;

import com.gitee.jenkins.gitee.hook.model.Action;
import com.gitee.jenkins.gitee.hook.model.State;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * @author Robin Müller
 */
public final class MergeRequestHookTriggerHandlerFactory {

    private MergeRequestHookTriggerHandlerFactory() {}

    public static MergeRequestHookTriggerHandler newMergeRequestHookTriggerHandler(boolean triggerOnOpenMergeRequest,
    		                                                                       boolean triggerOnUpdateMergeRequest,
    		                                                                       boolean triggerOnAcceptedMergeRequest,
    		                                                                       boolean triggerOnClosedMergeRequest,
                                                                                   boolean skipWorkInProgressMergeRequest,
                                                                                   boolean triggerOnApprovedMergeRequest,
                                                                                   boolean cancelPendingBuildsOnUpdate) {
        if (triggerOnOpenMergeRequest || triggerOnUpdateMergeRequest || triggerOnAcceptedMergeRequest || triggerOnClosedMergeRequest || triggerOnApprovedMergeRequest) {
        	return new MergeRequestHookTriggerHandlerImpl(retrieveAllowedStates(triggerOnOpenMergeRequest, triggerOnUpdateMergeRequest, triggerOnAcceptedMergeRequest, triggerOnClosedMergeRequest),
            											  retrieveAllowedActions(triggerOnOpenMergeRequest, triggerOnUpdateMergeRequest, triggerOnAcceptedMergeRequest, triggerOnClosedMergeRequest),
                                                          skipWorkInProgressMergeRequest, cancelPendingBuildsOnUpdate);
        } else {
            return new NopMergeRequestHookTriggerHandler();
        }
    }

	private static List<Action> retrieveAllowedActions(boolean triggerOnOpenMergeRequest,
                                                      boolean triggerOnUpdateMergeRequest,
                                                      boolean triggerOnAcceptedMergeRequest,
                                                      boolean triggerOnClosedMergeRequest) {
        List<Action> allowedActions =new ArrayList<>();

        if (triggerOnOpenMergeRequest) {
            allowedActions.add(Action.open);
        }

        if (triggerOnUpdateMergeRequest) {
            allowedActions.add(Action.update);
        }

		if (triggerOnAcceptedMergeRequest) {
            allowedActions.add(Action.merge);
        }

        if (triggerOnClosedMergeRequest) {
            allowedActions.add(Action.close);
        }

		return allowedActions;
	}

	private static List<State> retrieveAllowedStates(boolean triggerOnOpenMergeRequest,
			                                         boolean triggerOnUpdateMergeRequest,
			                                         boolean triggerOnAcceptedMergeRequest,
			                                         boolean triggerOnClosedMergeRequest) {
        List<State> result = new ArrayList<>();
        if (triggerOnOpenMergeRequest || triggerOnUpdateMergeRequest) {
            result.add(State.opened);
            result.add(State.open);
            result.add(State.reopened);
            result.add(State.updated);
        }

        if (triggerOnAcceptedMergeRequest)  {
        	result.add(State.merged);
        }
        if (triggerOnClosedMergeRequest) {
        	result.add(State.closed);
        }
        
        return result;
    }
}
