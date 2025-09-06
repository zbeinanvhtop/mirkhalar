#!/usr/bin/env bash

if [ "$TRAVIS_TAG" != "" ]; then
    ./mvnw versions::set -DnewVersion=${TRAVIS_TAG}
fi

 ./mvnw verify -Dmaven.javadoc.skip=true -B -Pall-tests,integration-test

