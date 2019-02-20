#!/usr/bin/env python
# -*- encoding: utf-8
"""
This script can "spike" an image on wellcomecollection.org/works -- remove
it from the site and prevent it from reappearing in a reindex.
"""

import datetime as dt
import getpass
import hashlib
import json
import os
import sys

import boto3
import requests


def sha256(bs):
    h = hashlib.sha256()
    h.update(bs)
    return h.hexdigest()


def platform_client(service_name):
    sts = boto3.client("sts")

    session_name = "%s--%s" % (getpass.getuser(), os.path.basename(__file__))

    resp = sts.assume_role(
        RoleArn="arn:aws:iam::760097843905:role/developer", RoleSessionName=session_name
    )

    credentials = resp["Credentials"]

    return boto3.client(
        service_name,
        aws_access_key_id=credentials["AccessKeyId"],
        aws_secret_access_key=credentials["SecretAccessKey"],
        aws_session_token=credentials["SessionToken"],
    )


def remove_image_from_es_indexes(catalogue_id):
    print("*** Removing the image from our Elasticsearch indexes")

    ecs_client = platform_client("ecs")
    ssm_client = platform_client("ssm")

    # AWLC: Yes, it would be more robust if we got this config by checking the
    # ingestor task definition to see what the current parameters are, but that's
    # such a faff with the namespace that I literally CBA.
    print("··· Reading Elasticsearch config for the ingestor (write credentials)")
    es_username = ssm_client.get_parameter(
        Name="/aws/reference/secretsmanager/catalogue/ingestor/es_username",
        WithDecryption=True,
    )
    es_password = ssm_client.get_parameter(
        Name="/aws/reference/secretsmanager/catalogue/ingestor/es_password",
        WithDecryption=True,
    )
    es_auth = (es_username["Parameter"]["Value"], es_password["Parameter"]["Value"])

    print("··· Getting the task definitions for the catalogue API")
    resp = ecs_client.list_services(cluster="catalogue-api")
    service_arns = resp["serviceArns"]
    assert len(service_arns) == 2

    resp = ecs_client.describe_services(cluster="catalogue-api", services=service_arns)
    services = resp["services"]
    assert len(services) == 2
    task_definitions = [service["taskDefinition"] for service in services]

    miro_id = None

    print("··· Reading Elastic Cloud config for the catalogue API (read credentials)")
    for td in task_definitions:
        resp = ecs_client.describe_task_definition(taskDefinition=td)
        container_definitions = resp["taskDefinition"]["containerDefinitions"]
        assert len(container_definitions) == 2

        app_containers = [cd for cd in container_definitions if cd["name"] == "app"]
        assert len(app_containers) == 1

        app_env_vars = {e["name"]: e["value"] for e in app_containers[0]["environment"]}

        app_secrets = {s["name"]: s["valueFrom"] for s in app_containers[0]["secrets"]}

        for name, value_from in app_secrets.items():
            resp = ssm_client.get_parameter(Name=value_from, WithDecryption=True)
            app_secrets[name] = resp["Parameter"]["Value"]

        # 3. Once we have the config and password, we can remove the work from
        # the Elasticsearch index (if present).
        es_host = "%s://%s:%s/" % (
            app_secrets["es_protocol"],
            app_secrets["es_host"],
            app_secrets["es_port"],
        )

        for index_name in (app_env_vars["es_index_v1"], app_env_vars["es_index_v2"]):
            index_name = "v2-2018-12-6-single-shard"
            print("··· Looking up %s in index %s" % (catalogue_id, index_name))
            resp = requests.get(
                "%s%s/%s/%s" % (es_host, index_name, index_name, catalogue_id),
                auth=es_auth,
            )

            if resp.status_code == 404:
                print("··· Work is not in this index, skipping")
                continue

            assert resp.status_code == 200, resp.json()

            existing_work = resp.json()["_source"]

            if existing_work["type"] == "IdentifiedInvisibleWork":
                print(
                    "··· Work is already suppressed as an IdentifiedInvisibleWork, skipping"
                )
                continue

            # While we're looking at API responses, try to get the Miro ID.
            identifiers = [existing_work["sourceIdentifier"]] + existing_work[
                "otherIdentifiers"
            ]
            miro_identifiers = [
                idf
                for idf in identifiers
                if idf["identifierType"]["id"] == "miro-image-number"
            ]
            assert len(miro_identifiers) == 1
            miro_identifier = miro_identifiers[0]["value"]
            if miro_id is None:
                miro_id = miro_identifier
            else:
                assert miro_id == miro_identifier, "Multiple Miro IDs? %s, %s" % (
                    miro_id,
                    miro_identifier,
                )

            assert existing_work["type"] == "IdentifiedWork"

            new_work = {
                "canonicalId": existing_work["canonicalId"],
                "sourceIdentifier": existing_work["sourceIdentifier"],
                "type": "IdentifiedInvisibleWork",
                # We bump the version so any in-flight works won't overwrite
                # this one.
                "version": existing_work["version"] + 1,
            }
            print("··· Replacing work with an IdentifiedInvisibleWork")
            resp = requests.put(
                "%s%s/%s/%s" % (es_host, index_name, index_name, catalogue_id),
                auth=es_auth,
                json=new_work,
            )
            resp.raise_for_status()

            resp = requests.get(
                "%s%s/%s/%s" % (es_host, index_name, index_name, catalogue_id),
                auth=es_auth,
            )
            assert resp.json()["_source"]["type"] == "IdentifiedInvisibleWork"

    return miro_id


def suppress_work_in_miro_vhs(miro_id):
    print("*** Marking the image as withdrawn in the Miro VHS")
    dynamodb_client = platform_client("dynamodb")

    resp = dynamodb_client.get_item(
        TableName="vhs-sourcedata-miro", Key={"id": {"S": miro_id}}
    )
    item = resp["Item"]

    if not item["isClearedForCatalogueAPI"]["BOOL"]:
        print("··· Work is already withdrawn in VHS")
        return

    item["isClearedForCatalogueAPI"]["BOOL"] = False
    item["version"]["N"] = str(int(item["version"]["N"]) + 1)

    # AWLC: I should do a conditional PutItem here because it's a VHS, but I CBA.
    # This table isn't usually changing much.
    resp = dynamodb_client.put_item(TableName="vhs-sourcedata-miro", Item=item)

    resp = dynamodb_client.get_item(
        TableName="vhs-sourcedata-miro", Key={"id": {"S": miro_id}}
    )
    assert not resp["Item"]["isClearedForCatalogueAPI"]["BOOL"]


def remove_image_from_loris_s3_bucket(miro_id):
    print("*** Removing the image from the Loris S3 bucket")
    s3_client = platform_client("s3")

    shard = miro_id[:-3] + "000"
    prefix = "%s/%s" % (shard, miro_id)
    bucket = "wellcomecollection-miro-images-public"
    print("··· Looking up objects under prefix s3://%s/%s" % (bucket, prefix))

    resp = s3_client.list_objects_v2(Bucket=bucket, Prefix=prefix)
    matching_keys = [obj["Key"] for obj in resp.get("Contents", [])]

    if not matching_keys:
        print("··· No matching objects in S3 bucket, skipping")
        return

    assert len(matching_keys) == 1, matching_keys
    key = matching_keys[0]
    assert key.endswith(".jpg")

    print("··· Detected object in S3 bucket, deleting: %s" % key)
    s3_client.delete_object(Bucket=bucket, Key=key)


def create_cloudfront_invalidations(miro_id):
    print("*** Creating a CloudFront invalidation for Loris")
    cloudfront_client = platform_client("cloudfront")

    resp = cloudfront_client.list_distributions()
    assert not resp["DistributionList"]["IsTruncated"]
    matching = [
        item
        for item in resp["DistributionList"]["Items"]
        if item["Origins"]["Items"][0]["DomainName"]
        == "iiif-origin.wellcomecollection.org"
    ]
    assert len(matching) == 1
    distribution_id = matching[0]["Id"]
    print("··· Detected Loris CloudFront distribution as %s" % distribution_id)

    url = "/image/%s.jpg/*" % miro_id
    print("··· Issuing an invalidation for %s" % url)

    resp = cloudfront_client.create_invalidation(
        DistributionId=distribution_id,
        InvalidationBatch={
            "Paths": {"Quantity": 1, "Items": [url]},
            "CallerReference": dt.datetime.now().isoformat(),
        },
    )
    assert resp["ResponseMetadata"]["HTTPStatusCode"] == 201


def update_miro_inventory(miro_id):
    print("*** Updating the Miro inventory")
    dynamodb_client = platform_client("dynamodb")
    s3_client = platform_client("s3")

    resp = dynamodb_client.get_item(
        TableName="vhs-miro-migration", Key={"id": {"S": miro_id}}
    )
    item = resp["Item"]

    s3_bucket = item["location"]["M"]["namespace"]["S"]
    s3_key = item["location"]["M"]["key"]["S"]
    print("··· Detected VHS inventory entry as s3://%s/%s" % (s3_bucket, s3_key))

    inventory_obj = s3_client.get_object(Bucket=s3_bucket, Key=s3_key)
    inventory_entry = json.load(inventory_obj["Body"])

    inventory_entry["catalogue_api_derivative"] = False
    inventory_entry["catalogue_api_derivative_bucket"] = None
    inventory_entry["catalogue_api_derivative_key"] = None

    new_entry = json.dumps(inventory_entry, separators=(",", ":")).encode("utf8")
    new_key = "%s/%s.json" % (miro_id, sha256(new_entry))
    if new_key == s3_key:
        print("··· Inventory is already up to date, skipping")
        return

    print("··· Updating VHS inventory entry")
    s3_client.put_object(Bucket=s3_bucket, Key=new_key, Body=new_entry)

    item["location"]["M"]["key"]["S"] = new_key
    item["version"]["N"] = str(int(item["version"]["N"]) + 1)

    resp = dynamodb_client.put_item(TableName="vhs-miro-migration", Item=item)


if __name__ == "__main__":
    catalogue_id = sys.argv[1]
    print("*** Suppressing Miro ID %s" % catalogue_id)

    miro_id = remove_image_from_es_indexes(catalogue_id=catalogue_id)
    assert miro_id is not None, "Don't know the Miro ID!"
    print("*** Detected Miro ID as %s" % miro_id)

    suppress_work_in_miro_vhs(miro_id)

    remove_image_from_loris_s3_bucket(miro_id)
    create_cloudfront_invalidations(miro_id)
    update_miro_inventory(miro_id)

    print(
        "*** You also need to (manually) create a CloudFront invalidation for the /works page on wellcomecollection.org"
    )
