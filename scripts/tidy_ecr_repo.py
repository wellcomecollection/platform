#!/usr/bin/env python
# -*- encoding: utf-8

import datetime as dt
import getpass
import itertools

import boto3
import click


def role_arn_to_session(**kwargs):
    client = boto3.client("sts")
    response = client.assume_role(**kwargs)
    return boto3.Session(
        aws_access_key_id=response["Credentials"]["AccessKeyId"],
        aws_secret_access_key=response["Credentials"]["SecretAccessKey"],
        aws_session_token=response["Credentials"]["SessionToken"],
    )


def describe_images(ecr_client, repo_name):
    paginator = ecr_client.get_paginator("describe_images")
    for page in paginator.paginate(repositoryName=repo_name):
        yield from page["imageDetails"]


@click.command()
@click.argument("repo_name")
@click.option("--account_id", default="760097843905")
@click.option("--older_than", default=500, type=int)
def main(repo_name, account_id, older_than):
    sess = role_arn_to_session(
        RoleArn="arn:aws:iam::%s:role/admin" % account_id,
        RoleSessionName="%s--%s" % (getpass.getuser(), __file__),
    )
    ecr_client = sess.client("ecr")

    images_to_delete = []

    full_repo_name = "uk.ac.wellcome/%s" % repo_name
    for image in describe_images(ecr_client, repo_name=full_repo_name):
        when_pushed = dt.datetime.now(dt.timezone.utc) - image["imagePushedAt"]
        if when_pushed.days > 500:
            images_to_delete.append({"imageDigest": image["imageDigest"]})

    click.confirm("About to delete %d images" % len(images_to_delete))

    for batch in chunked_iterable(images_to_delete, size=100):
        ecr_client.batch_delete_image(repositoryName=full_repo_name, imageIds=batch)


def chunked_iterable(iterable, size):
    it = iter(iterable)
    while True:
        chunk = tuple(itertools.islice(it, size))
        if not chunk:
            break
        yield chunk


if __name__ == "__main__":
    main()
