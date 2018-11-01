#!/usr/bin/env python
# -*- encoding: utf-8

import boto3
import hcl
import requests


def get_terraform_vars():
    s3_client = boto3.client("s3")
    tfvars_body = s3_client.get_object(
        Bucket="wellcomecollection-platform-infra",
        Key="terraform.tfvars"
    )["Body"]
    return hcl.load(tfvars_body)


def build_url(es_credentials):
    protocol = es_credentials["protocol"]
    name = es_credentials["name"]
    region = es_credentials["region"]
    port = es_credentials["port"]
    return f"{protocol}://{name}.{region}.aws.found.io:{port}"


def get_all_indexes(es_url, username, password):
    resp = requests.get(
        f"{es_url}/_cat/indices",
        auth=(username, password),
        params={"format": "json"}
    )
    resp.raise_for_status()

    return resp.json()


if __name__ == "__main__":
    terraform_vars = get_terraform_vars()
    es_cluster_credentials = terraform_vars["es_cluster_credentials"]

    es_url = build_url(es_cluster_credentials)

    username = es_cluster_credentials["username"]
    password = es_cluster_credentials["password"]

    indexes = get_all_indexes(es_url, username=username, password=password)

    print(
        '\n'.join(sorted(
            idx["index"]
            for idx in indexes
            if not idx["index"].startswith(".")
        ))
    )
