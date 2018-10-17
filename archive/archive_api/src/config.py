# -*- encoding: utf-8

import os

import boto3
import requests


class ArchiveAPIConfig(object):

    DYNAMODB_RESOURCE = boto3.resource("dynamodb")
    SNS_CLIENT = boto3.client("sns")
    S3_CLIENT = boto3.client("s3")
    PROGRESS_MANAGER_SESSION = requests.Session()
    BAGS_MANAGER_SESSION = requests.Session()

    # Disable Flask-RESTPlus including the "message" field on errors.
    # See https://flask-restplus.readthedocs.io/en/stable/errors.html
    ERROR_INCLUDE_MESSAGE = False

    def __init__(self, development=False):
        try:
            if development:
                self.SNS_TOPIC_ARN = (
                    "arn:aws:sns:eu-west-1:760097843905:archive-storage_archivist"
                )
                self.BAG_VHS_BUCKET_NAME = "wellcomecollection-vhs-archive-manifests"
                self.BAG_VHS_TABLE_NAME = "vhs-archive-manifests"
                self.PROGRESS_MANAGER_ENDPOINT = "http://localhost:6000"
                self.BAGS_MANAGER_ENDPOINT = "http://localhost:6001"
            else:
                self.SNS_TOPIC_ARN = os.environ["TOPIC_ARN"]
                self.BAG_VHS_BUCKET_NAME = os.environ["BAG_VHS_BUCKET_NAME"]
                self.BAG_VHS_TABLE_NAME = os.environ["BAG_VHS_TABLE_NAME"]
                self.PROGRESS_MANAGER_ENDPOINT = os.environ["PROGRESS_MANAGER_ENDPOINT"]
                self.BAGS_MANAGER_ENDPOINT = os.environ["BAGS_MANAGER_ENDPOINT"]
        except KeyError as err:
            raise RuntimeError(f"Unable to create config: {err!r}")
