package com.dabsquared.gitlabjenkins.trigger.filter;

import org.junit.Test;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.FromDataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Robin Müller
 */
@RunWith(Theories.class)
public class RegexBasedFilterTest {

    @DataPoints("matching-branches")
    public static String[] matchingBranchNames = {"feature/test", "feature/awesome-feature"};

    @DataPoints("not-matching-branches")
    public static String[] notMatchingBranchNames = {"hotfix/test", "hotfix/awesome-feature", "master", "develop"};

    @Theory
    public void isRegexBranchAllowed(@FromDataPoints("matching-branches") String branchName) {
        RegexBasedFilter featureBranches = new RegexBasedFilter("feature/.*");

        assertThat(featureBranches.isBranchAllowed(branchName), is(true));
    }

    @Theory
    public void isRegexBranchNotAllowed(@FromDataPoints("not-matching-branches") String branchName) {
        RegexBasedFilter featureBranches = new RegexBasedFilter("feature/.*");

        assertThat(featureBranches.isBranchAllowed(branchName), is(false));
    }

    @Test
    public void getConfig() {
        String regex = "test.*";

        BranchFilterConfig config = new RegexBasedFilter(regex).getConfig();

        assertThat(config.getType(), is(BranchFilterType.RegexBasedFilter));
        assertThat(config.getIncludeBranchesSpec(), nullValue());
        assertThat(config.getExcludeBranchesSpec(), nullValue());
        assertThat(config.getTargetBranchRegex(), is(regex));
    }
}
