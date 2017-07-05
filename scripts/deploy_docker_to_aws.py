#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Push a Docker image to ECR and upload a release ID to S3.

Usage:
  deploy_docker_to_aws.py --docker-image=<name> --infra-bucket=<bucket>
  deploy_docker_to_aws.py -h | --help

Options:
  -h --help                Show this screen.
  --docker-image=<image>   Tag of the docker image to push to ECR
                           (e.g. nginx:83a21169d2bfdac703c4fce0bcc565b3a3816abf)
  --infra-bucket=<bucket>  Name of the infra bucket for storing release IDs.
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

    # Parse the ID of the Docker image.  This is of the form:
    #
    #      {project}:{tag}
    #
    docker_image = args['--docker-image']
    project, tag = docker_image.split(':')
    print(f'*** Pushing {docker_image!r} to AWS')

    # Look up the URI of our ECR repo -- this is needed for authentication
    # and for pushing.
    repo_name = f'uk.ac.wellcome/{project}'
    repo_uri = ecr_repo_uri_from_name(ecr_client, name=repo_name)
    print(f'*** ECR repo URI is {repo_uri!r}')

    print('*** Authenticating for `docker push` with ECR')
    authenticate_for_ecr_pushes(
        ecr_client=ecr_client,
        docker_client=docker_client,
        repo_uri=repo_uri
    )

    # Now retag the image, prepending our ECR URI.  When we're done, we'll
    # delete the retagged image, to avoid clogging up the local image registry.
    renamed_image_tag = f'{repo_uri}:{tag}'
    print(f'*** Pushing image {docker_image!r} to ECR')
    try:
        image = docker_client.images.get(docker_image)
        image.tag(repository=repo_uri, tag=tag)
        docker_client.images.push(repository=repo_uri, tag=tag)
    finally:
        docker_client.images.remove(image=renamed_image_tag)

    # Finally, upload the release ID string to S3.
    print(f'*** Uploading release ID to S3')
    bucket.upload_file(
        Filename=os.path.join(ROOT, '.releases', project),
        Key=f'releases/{project}'
    )
