package com.dabsquared.gitlabjenkins.testhelpers;

import com.dabsquared.gitlabjenkins.model.PushHook;

public interface GitLabPushRequestSamples {
	PushHook pushBrandNewMasterBranchRequest();

	PushHook pushNewBranchRequest();

	PushHook pushCommitRequest();

	PushHook mergePushRequest();

	PushHook pushNewTagRequest();

	PushHook deleteBranchRequest();
}
