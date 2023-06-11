package com.dabsquared.gitlabjenkins.trigger.filter;

import org.apache.commons.lang.StringUtils;

import static com.dabsquared.gitlabjenkins.trigger.filter.BranchFilterConfig.BranchFilterConfigBuilder.branchFilterConfig;

/**
 * @author Robin Müller
 */
class RegexBasedFilter implements BranchFilter {

    private final String regex;

    public RegexBasedFilter(String regex) {
        this.regex = regex;
    }

    @Override
    public boolean isBranchAllowed(String branchName) {
        return StringUtils.isEmpty(branchName) || StringUtils.isEmpty(regex) || branchName.matches(regex);
    }

    @Override
    public BranchFilterConfig getConfig() {
        return branchFilterConfig().withTargetBranchRegex(regex).build(BranchFilterType.RegexBasedFilter);
    }

}
