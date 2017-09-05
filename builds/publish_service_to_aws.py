#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Push a Docker image to ECR and upload a release ID to S3.

Usage:
  publish_service_to_aws.py --project=<name> --infra-bucket=<bucket>
  publish_service_to_aws.py -h | --help

Options:
  -h --help                Show this screen.
  --project=<project>      Name of the project (e.g. api, loris).  Assumes
                           there's a Docker image of the same name.
  --infra-bucket=<bucket>  Name of the infra bucket for storing release IDs.

This script looks up the release ID (which it assumes is the Docker tag)
from the .releases directory in the root of the repo.

"""

import os
import subprocess

import boto3
import docopt

from tooling import ecr_login, ecr_repo_uri_from_name, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    s3 = boto3.resource('s3')
    bucket = s3.Bucket(args['--infra-bucket'])
    ecr_client = boto3.client('ecr')

    project = args['--project']
    print('*** Pushing %s to AWS' % project)

    # Get the release ID (which is the image tag)
    release_file = os.path.join(ROOT, '.releases', project)
    release_file_exists = True
    try:
        tag = open(release_file).read().strip()
    except FileNotFoundError:
        release_file_exists = False
        tag = 'latest'
    docker_image = '%s:%s' % (project, tag)

    # Look up the URI of our ECR repo -- this is needed for authentication
    # and for pushing.
    repo_name = 'uk.ac.wellcome/%s' % project
    repo_uri = ecr_repo_uri_from_name(ecr_client, name=repo_name)
    print('*** ECR repo URI is %s' % repo_uri)

    print('*** Authenticating for `docker push` with ECR')
    ecr_login()

    # Now retag the image, prepending our ECR URI.  When we're done, we'll
    # delete the retagged image, to avoid clogging up the local image registry.
    renamed_image_tag = '%s:%s' % (repo_uri, tag)
    print('*** Pushing image %s to ECR' % docker_image)
    try:
        subprocess.check_call(['docker', 'tag', docker_image, renamed_image_tag])
        subprocess.check_call(['docker', 'push', renamed_image_tag])
    finally:
        subprocess.check_call(['docker', 'rmi', renamed_image_tag])

    # Finally, upload the release ID string to S3.
    if release_file_exists:
        print('*** Uploading release ID to S3')
        bucket.upload_file(Filename=release_file, Key='releases/%s' % project)
