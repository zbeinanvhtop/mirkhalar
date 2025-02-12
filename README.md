# Table of Contents
- [Introduction](#introduction)
- [User support](#user-support)
- [Known bugs/issues](#known-bugsissues)
- [Supported GitLab versions](#supported-gitlab-versions)
- [Supported GitLabCI Functions](#supported-gitlabci-functions)
- [Configuring access to GitLab](#configuring-access-to-gitlab)
    - [Jenkins Job Configuration](#jenkins-job-configuration)
    - [Gitlab Configuration (>= 8.1)](#gitlab-configuration)
- [Branch filtering](#branch-filtering)
- [Build Tags](#build-tags)
- [Parameterized builds](#parameterized-builds)
- [Contributing to the Plugin](#contributing-to-the-plugin)
- [Quick test environment setup using Docker](#quick-test-environment-setup-using-docker)
    - [Access GitLab](#access-gitlab)
    - [Access Jenkins](#access-jenkins)
- [Release Workflow](#release-workflow)

# Introduction

This plugin allows GitLab to trigger builds in Jenkins after code is pushed and/or after a merge request is created.

# User support

If you have a problem or question about using the plugin, please make sure you are using the latest version. Then create an issue in the GitHub project if necessary. New issues should include the following:
* GitLab plugin version (e.g. 1.2.0)
* GitLab version (e.g. 8.1.1)
* Jenkins version (e.g. 1.651.1)
* Relevant log output from the plugin (see below for instructions on capturing this)

Version 1.2.0 of the plugin introduced improved logging for debugging purposes. To enable it: 

1. Go to Jenkins -> Manage Jenkins -> System Log
2. Add new log recorder
3. Enter 'Gitlab plugin' or whatever you want for the name
4. On the next page, enter 'com.dabsquared.gitlabjenkins' for Logger, set log level to FINEST, and save
5. Then click on your Gitlab plugin log, click 'Clear this log' if necessary, and then use GitLab to trigger some actions
6. Refresh the log page and you should see output

You can also try chatting with us in the #gitlab-plugin channel on the Freenode IRC network.

# Known bugs/issues

This is not an exhaustive list of issues, but rather a place for us to note significant bugs that may impact your use of the plugin in certain circumstances. For most things, please search the [Issues](https://github.com/jenkinsci/gitlab-plugin/issues) section and open a new one if you don't find anything.
* [#272](https://github.com/jenkinsci/gitlab-plugin/issues/272) - Plugin version 1.2.0+ does not work with GitLab Enterprise Edition < 8.8.3, due to a bug on their side.
* Jenkins versions 1.651.2 and 2.3 removed the ability of plugins to set arbitrary job parameters that are not specifically defined in each job's configuration. This was an important security update, but it has broken compatibility with some plugins, including ours. See [here](https://jenkins.io/blog/2016/05/11/security-update/) for more information and workarounds if you are finding parameters unset or empty that you expect to have values.

# Supported GitLab versions

* GitLab versions 8.1.x and newer (both CE and EE editions) are supported via the GitLab commit status API which supports with external CI services like Jenkins
* Versions older than 8.1.x may work but are no longer officially supported

# Configuring access to GitLab

Optionally, the plugin communicates with the GitLab server in order to fetch additional information. At this moment, this information is limited to fetching the source project of a Merge Request, in order to support merging from forked repositories.

To enable this functionality, a user should be set up on GitLab, with GitLab 'Developer' permissions, to access the repository. You will need to give this user access to each repo you want Jenkins to be able to clone. Log in to GitLab as that user, go to its profile, and copy its secret API key. On the Global Configuration page in Jenkins, supply the GitLab host URL, e.g. ``http://your.gitlab.server.`` Click the 'Add' button to add a credential, choose 'GitLab API token' as the kind of credential, and paste your GitLab user's API key into the 'API token' field. Testing the connection should succeed.

## Jenkins Job Configuration
### Git configuration for Freestyle jobs
1. In the *Source Code Management* section:
    1. Click *Git*
    2. Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)
      * In the Advanced settings, set its *Name* to ``origin`` and its *refspec* to ``+refs/heads/*:refs/remotes/origin/* +refs/merge-requests/*/head:refs/remotes/origin/merge-requests/*``
    3. To be able to merge from forked repositories:  <br/>**Note:** this requires [configuring communication to the GitLab server](#configuring-access-to-gitlab)
      * Add a second repository with:
        * *URL*: ``${gitlabSourceRepoURL}`` 
        * *Name* (in Advanced): ``${gitlabSourceRepoName}``
    4. In *Branch Specifier* enter:
      * For single-repository setups: ``origin/${gitlabSourceBranch}``
      * For forked repository setups: ``merge-requests/${gitlabMergeRequestIid}``
    5. In *Additional Behaviours*:
        * Click the *Add* drop-down button.
        * Select *Merge before build* from the drop-down.
        * Set *Name of the repository" to ``origin`` 
        * Set *Branch to merge* as ``${gitlabTargetBranch}``

**Note:** Since version **1.2.0** the *gitlab-plugin* sets the gitlab hook values through *environment variables* instead of *build parameters*. To set default values, consult [EnvInject Plugin](https://wiki.jenkins-ci.org/display/JENKINS/EnvInject+Plugin).

### Git configuration for Pipeline/Workflow jobs
**Incompatibility note:** When upgrading to version 1.2.1 or later of the plugin, if you are using Pipeline jobs you will need to manually reconfigure your Pipeline scripts. In older versions the plugin set global Groovy variables that could be accessed as e.g. ${gitlabSourceBranch}. After version 1.2.1, these variables are only accessible in the env[] map. E.g. ${env.gitlabSourceBranch}. 

* A Jenkins Pipeline bug will prevent the Git clone from working when you use a Pipeline script from SCM. It works if you use the Jenkins job config UI to edit the script. There is a workaround mentioned here: https://issues.jenkins-ci.org/browse/JENKINS-33719

* Use the Snippet generator, General SCM step, to generate sample Groovy code for the git checkout/merge etc.
* Example that performs merge before build: `checkout changelog: true, poll: true, scm: [$class: 'GitSCM', branches: [[name: "origin/${env.gitlabSourceBranch}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'PreBuildMerge', options: [fastForwardMode: 'FF', mergeRemote: 'origin', mergeStrategy: 'default', mergeTarget: "${env.gitlabTargetBranch}"]]], submoduleCfg: [], userRemoteConfigs: [[name: 'origin', url: 'git@mygitlab:foo/testrepo.git']]]`

### Git configuration for Multibranch Pipeline/Workflow jobs
**Note:** none of the GitLab environment variables are available for mulitbranch pipeline jobs as there is no way to pass some additional data to a multibranch pipeline build while notifying a multibranch pipeline job about SCM changes.
Due to this the plugin just listens for GitLab Push Hooks for multibranch pipeline jobs; Merge Request hooks are ignored.

1. Click **Add source**
2. Select **Git**
3. Enter your *Repository URL* (e.g.: ``git@your.gitlab.server:group/repo_name.git``)
4. Unlike other job types, there is no 'Trigger' setting required for a Multibranch job configuration; just create a webhook in GitLab for push requests which points to ``http://JENKINS_URL/project/PROJECT_NAME``

Example `Jenkinsfile` for multibranch pipeline jobs
```
// Reference the GitLab connection name from your Jenkins Global configuration (http://JENKINS_URL/configure, GitLab section)
properties([[$class: 'GitLabConnectionProperty', gitLabConnection: '<your-gitlab-connection-name']])

node {
    stage "checkout"
    checkout scm

    stage "build"
    gitlabCommitStatus("build") {
        // your build steps
    }

    stage "test"
    gitlabCommitStatus("test") {
        // your test steps
    }
}
```

### Freestyle and Pipeline jobs
1. In the *Build Triggers* section:
    * Check the ``Build when a change is pushed to GitLab.``
    * Use the check boxes to trigger builds on Push and/or Merge Request events and/or Note Request(Comment in Merge Request)
    * Optionally enable building open merge requests again after a push to the source branch.
    * If you check *Note Events* then you should set trigger phrase(exact phrase or Java Regular Expression) in Trigger Phrase field.
2. Configure any other pre build, build or post build actions as necessary
3. Click *Save* to preserve your changes in Jenkins.

### Matrix/Multi-configuration jobs
**The Jenkins Matrix/Multi-configuration job type is not supported.**

## Gitlab Configuration

GitLab 8.1 has implemented a commit status api, you need an extra post-build step to support commit status.

* In GitLab go to you repository's project *Settings*
    * Click on *Web Hooks*
    * Add a Web Hook for *Merge Request Events* and *Push Events* to ``http://JENKINS_URL/project/PROJECT_NAME`` <br/>

* Add a post-build step ``Publish build status to GitLab commit (GitLab 8.1+ required)`` to the job.
* For pipeline jobs surround your build step with the gitlabCommitStatus step like this:

    ```
    node() {
        stage 'Checkout'
        checkout <your-scm-config>

        gitlabCommitStatus {
           <script that builds, tests, etc. your project>
        }
    }
    ```
* For pipeline jobs there is also the updateGitlabCommitStatus step to use a custom state for updating the commit status:

    ```
    node() {
        stage 'Checkout'
        checkout <your-scm-config>

        updateGitlabCommitStatus name: 'build', state: 'pending'
    }
    ```
* To mark several build stages as pending in GitLab you can use the gitlabBuilds step:

    ```
    node() {
        stage 'Checkout'
        checkout <your-scm-config>

        gitlabBuilds(builds: ["build", "test"]) {
            stage "build"
            gitlabCommitStatus("build") {
                // your build steps
            }

            stage "test"
            gitlabCommitStatus("test") {
                // your test steps
            }
        }
    }
    ```
* Configure access to GitLab as described above in ["Configure access to GitLab"](#configuring-access-to-gitlab) (the account needs at least developer permissions to post commit statuses)

# Branch filtering

Triggers may be filtered based on the branch name, i.e. the build will only be allowed for selected branches. On the project configuration page, when you configure the GitLab trigger, you can choose 'Filter branches by name' or 'Filter branches by regex.' Filter by name takes comma-separated lists of branch names to include and/or exclude from triggering a build. Filter by regex takes a Java regular expression to include and/or exclude.

**Note:** This functionality requires accessing the GitLab server (see [above](#configuring-access-to-gitlab)) and for the time being also a git repository url already saved in the project configuration. In other words, when creating a new project, the configuration needs to be saved *once* before being able to select the allowed branches. For Workflow/Pipeline jobs, the configuration must be saved *and* the job must be run once before the list is populated. For existing projects, all branches are allowed to push by default.

# Build Tags

In order to build when a new tag is pushed:
* In the ``GitLab server`` add ``Tag push events`` to the ``Web Hook``
* In the ``Jenkins`` under the ``Source Code Management`` section:
    * select ``Advance...`` and add  ``+refs/tags/*:refs/remotes/origin/tags/*`` as ``Refspec``
    * you can also use ``Branch Specifier`` to specify which tag need to be built (exampple ``refs/tags/${TAGNAME}``)

# Send message on complete of a build

1. In the *Post build steps* section:
    1. Click *Add post build step*
    2. Click *Add note with build status on GitLab merge requests* and save build settings (You enabled autoumatically sending default message on result of a build)

2. If you want make custom message on result of a build:
    1. In *Add note with build status on GitLab merge requests* section click to *Custom message on success/failure/abort*
    2. Write text of message, you can use Environment variables

# Parameterized builds

You can trigger a job a manually by clicking ``This build is parameterized`` and adding the any of the relevant build parameters.
These include:

* gitlabBranch
* gitlabSourceBranch
* gitlabActionType
* gitlabUserName
* gitlabUserEmail
* gitlabSourceRepoHomepage
* gitlabSourceRepoName
* gitlabSourceNamespace
* gitlabSourceRepoURL
* gitlabSourceRepoSshUrl
* gitlabSourceRepoHttpUrl
* gitlabMergeRequestTitle
* gitlabMergeRequestDescription
* gitlabMergeRequestId
* gitlabMergeRequestIid
* gitlabMergeRequestLastCommit
* gitlabTargetBranch
* gitlabTargetRepoName
* gitlabTargetNamespace
* gitlabTargetRepoSshUrl
* gitlabTargetRepoHttpUrl
* gitlabBefore
* gitlabAfter
* gitlabTriggerPhrase

# Contributing to the Plugin

Plugin source code is hosted on [Github](https://github.com/jenkinsci/gitlab-plugin).
New feature proposals and bug fix proposals should be submitted as
[Github pull requests](https://help.github.com/articles/creating-a-pull-request).
Fork the repository on Github, prepare your change on your forked
copy, and submit a pull request (see [here](https://github.com/jenkinsci/gitlab-plugin/pulls) for open pull requests). Your pull request will be evaluated by the [Cloudbees Jenkins job](https://jenkins.ci.cloudbees.com/job/plugins/job/gitlab-plugin/).

If you are adding new features please make sure that they support the Jenkins Workflow Plugin.
See [here](https://github.com/jenkinsci/workflow-plugin/blob/master/COMPATIBILITY.md) for some information.

Before submitting your change make sure that:
* your changes work with the oldest and latest supported GitLab version
* new features are provided with tests
* refactored code is provided with regression tests
* the code formatting follows the plugin standard
* imports are organised
* you updated the help docs
* you updated the README
* you have used findbugs to see if you haven't introduced any new warnings.

# Quick test environment setup using Docker

In order to test the plugin on different versions of `GitLab` and `Jenkins` you may want to use `Docker` containers.

A example docker-compose file is available at `gitlab-plugin/src/docker` which allows to set up instances of the latest `GitLab` and `Jenkins` versions.

To start the containers, run below command from the `docker` folder:

```bash
docker-compose up -d
```

## Access GitLab

To access `GitLab`, point your browser to `http://172.17.0.1:10080` and set a password for the `root` user account.

For more information on the supported `GitLab` versions and how to configure the containers, visit Sameer Naik's github page at https://github.com/sameersbn/docker-gitlab.

## Access Jenkins

To see `Jenkins`, point your browser to `http://localhost:8080`.

For more information on the supported `Jenkins` tags and how to configure the containers, visit https://hub.docker.com/r/library/jenkins.

# Release Workflow

GitLab-Plugin admins should adhere to the following rules when releasing a new plugin version:

* Ensure codestyle conformity
* Run unit tests
* Run manual tests on both, oldest and latest GitLab versions
* Update documentation
* Create change log
* Create release tag
* Create release notes (on github)
