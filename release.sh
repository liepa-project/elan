#!/bin/bash
current_hash=$(git rev-parse --short HEAD)
release_version="6.9.2"


echo "current hash: $current_hash, release version: $release_version"

git tag -a release-liepa3-${release_version} $current_hash -m"tagging ${release_version}"

git push origin tag release-liepa3-${release_version}
