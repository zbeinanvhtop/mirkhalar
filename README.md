gitlab-jenkins-plugin
=====================

This plugin emulates Jenkins as a GitlabCI Web Service to be used with GitlabHQ.

[![Build Status](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin.svg?branch=master)](https://travis-ci.org/DABSquared/gitlab-jenkins-plugin) 
[![Gitter chat](https://badges.gitter.im/DABSquared/gitlab-jenkins-plugin.png)](https://gitter.im/DABSquared/gitlab-jenkins-plugin)


Current Supported GitLabCI Functions
=====================
* `/project/PROJECT_NAME/builds/COMMIT_SHA1/status.json`
* `/project/PROJECT_NAME/builds/status.png?ref=BRANCH_NAME`
* `/project/PROJECT_NAME/builds/status.png?sha1=COMMIT_SHA1`
* `/project/PROJECT_NAME/builds/COMMIT_SHA1` redirects to build page.


* `/project/PROJECT_NAME`    In order for it to build properly on push you need to add this as a seperate web hook for just merge requests.

Major Help Needed
=====================
I would like this project to be able to handle building merge requests and regular pushes. In order to do this I need a way to configure the git plugin via code to merge two branches together before a build. Much like the RevisionParameterAction.java in the git plugin, we need a class that takes to branches, a source and a target, and can be passed as a build action. I have started an issue for the Git plugin here: https://issues.jenkins-ci.org/browse/JENKINS-23362 If you know of a way to do this please PM on twitter at @bass_rock. All the other necessary code exists in this repo and works.

Using it With A Job
=====================
* Create a new job by going to ``New Job``
* Set the ``Project Name``
* Feel free to specify the ``GitHub Project`` url as the url for the Gitlab project (if you have the GitHub plugin installed)
* In the ``Source Code Management`` section:
    * Click ``Git`` and enter your Repositroy URL and in Advanced set its Name to ``origin``
    * In ``Branch Specifier`` enter ``origin/${gitlabSourceBranch}``
    * In the ``Additional Behaviours`` section:
        * Click the ``Add`` drop down button and the ``Merge before build`` item
        * Specify the name of the repository as ``origin`` (if origin corresponds to Gitlab) and enter the ``Branch to merge to`` as ``${gitlabTargetBranch}``
* In the ``Build Triggers`` section:
    * Check the ``Build when a change is pushed to GitLab.``
* In GitLab go to the project ``Settings``
    * Click on ``Services``
    * Click on ``GitLab CI``
        * For ``token`` put any random string (This is not yet functioning)
        * For ``Project URL`` put ``http://JENKINS_URL/project/PROJECT_NAME``
    * Click on ``Web Hooks``
        * Add a ``Web Hook`` for ``Merge Request Events`` to ``http://JENKINS_URL/project/PROJECT_NAME``  (GitLab for some reason does not send a merge request event with the GitLab Service)
* Configure any other pre build, build or post build actions as necessary
* ``Save`` to preserve your changes

You can trigger a job a manually by clicking ``This build is parameterized`` and adding the relevant build parameters.
These include:

* gitlabSourceBranch
* gitlabTargetBranch
* gitlabBranch (This is optional and can be used in shell scripts for the branch being built by the push request)


Help Needed
=====================

* `/projects/` - seems to be already used by Jenkins, A way to use this path would be awesome
* `?token=XYZ` - Can not find a way to include a token parameter on an AbstractProject to security check without an extra plugin configuration
* `/PROJECT_NAME/`  should really be /PROJECT_ID_NUMBER/ - Can not find a project id number on an AbstractProject to use here instead.


Known Issues
=====================
* GitLab CI Merge Status pages says pending when there is no build scheduled, or the status is unknown. This is because I coded a workaround until this bug gets resolved: https://github.com/gitlabhq/gitlabhq/issues/7047


Contributing
=====================

1. Fork it ( https://github.com/[my-github-username]/gitlab-jenkins-plugin/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create a new Pull Request

Contributors
=====================

* @bass_rock, base ground work, primary developer.
* @DABSquared, company sponsoring development.
* @xaniasd, suggested a temporary work around for merge requests on Gitter.
