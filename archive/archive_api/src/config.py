# -*- encoding: utf-8

import os

import boto3
import requests


class ArchiveAPIConfig(object):

    DYNAMODB_RESOURCE = boto3.resource("dynamodb")
    SNS_CLIENT = boto3.client("sns")
    S3_CLIENT = boto3.client("s3")
    PROGRESS_MANAGER_SESSION = requests.Session()

    def __init__(self, development=False):
        try:
            if development:
                self.DYNAMODB_TABLE_NAME = "archive-storage-progress-table"
                self.SNS_TOPIC_ARN = (
                    "arn:aws:sns:eu-west-1:760097843905:archive-storage_archivist"
                )
                self.BAG_VHS_BUCKET_NAME = "wellcomecollection-vhs-archive-manifests"
                self.BAG_VHS_TABLE_NAME = "vhs-archive-manifests"
                self.PROGRESS_MANAGER_ENDPOINT = "http://localhost:6000"
            else:
                self.DYNAMODB_TABLE_NAME = os.environ["TABLE_NAME"]
                self.SNS_TOPIC_ARN = os.environ["TOPIC_ARN"]
                self.BAG_VHS_BUCKET_NAME = os.environ["BAG_VHS_BUCKET_NAME"]
                self.BAG_VHS_TABLE_NAME = os.environ["BAG_VHS_TABLE_NAME"]
                self.PROGRESS_MANAGER_ENDPOINT = os.environ["PROGRESS_MANAGER_ENDPOINT"]
        except KeyError as err:
            raise RuntimeError(f"Unable to create config: {err!r}")
