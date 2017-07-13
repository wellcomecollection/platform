#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Push a Docker image to ECR and upload a release ID to S3.

Usage:
  deploy_docker_to_aws.py --project=<name> --infra-bucket=<bucket>
  deploy_docker_to_aws.py -h | --help

Options:
  -h --help                Show this screen.
  --project=<project>      Name of the project (e.g. api, loris).  Assumes
                           there's a Docker image of the same name.
  --infra-bucket=<bucket>  Name of the infra bucket for storing release IDs.

This script looks up the release ID (which it assumes is the Docker tag)
from the .releases directory in the root of the repo.

"""

import os

import boto3
import docker
import docopt

from tooling import authenticate_for_ecr_pushes, ecr_repo_uri_from_name, ROOT


if __name__ == '__main__':
    args = docopt.docopt(__doc__)

    s3 = boto3.resource('s3')
    bucket = s3.Bucket(args['--infra-bucket'])
    ecr_client = boto3.client('ecr')
    docker_client = docker.from_env()

    project = args['--project']
    print('*** Pushing %s to AWS' % project)

    # Get the release ID (which is the image tag)
    release_file = os.path.join(ROOT, '.releases', project)
    tag = open(release_file).read().strip()
    docker_image = '%s:%s' % (project, tag)

    # Look up the URI of our ECR repo -- this is needed for authentication
    # and for pushing.
    repo_name = 'uk.ac.wellcome/%s' % project
    repo_uri = ecr_repo_uri_from_name(ecr_client, name=repo_name)
    print('*** ECR repo URI is %s' % repo_uri)

    print('*** Authenticating for `docker push` with ECR')
    authenticate_for_ecr_pushes(
        ecr_client=ecr_client,
        docker_client=docker_client,
        repo_uri=repo_uri
    )

    # Now retag the image, prepending our ECR URI.  When we're done, we'll
    # delete the retagged image, to avoid clogging up the local image registry.
    renamed_image_tag = '%s:%s' % (repo_uri, tag)
    print('*** Pushing image %s to ECR' % docker_image)
    try:
        image = docker_client.images.get(docker_image)
        image.tag(repository=repo_uri, tag=tag)
        resp = docker_client.images.push(repository=repo_uri, tag=tag)
        print(resp)
    finally:
        docker_client.images.remove(image=renamed_image_tag)

    # Finally, upload the release ID string to S3.
    print('*** Uploading release ID to S3')
    bucket.upload_file(Filename=release_file, Key='releases/%s' % project)
