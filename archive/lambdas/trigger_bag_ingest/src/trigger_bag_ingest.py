#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This lambda sends a bag for ingest to test the ingest and storage service is working
"""
import os
import boto3
import json
import daiquiri
from base64 import b64decode
from wellcome_aws_utils.lambda_utils import log_on_error
from wellcome_storage_client import WellcomeStorageClient


class Config:
    def __init__(
        self, bag_paths, storage_space, api_url, ingests_bucket, oauth_details_enc
    ):
        self.bag_paths = [b.strip() for b in bag_paths.split(",")]
        self.storage_space = storage_space
        self.api_url = api_url
        self.ingests_bucket = ingests_bucket

        self.oauth_details = None
        if oauth_details_enc:
            try:
                plain_text = self.decrypt(oauth_details_enc)
                self.oauth_details = json.loads(plain_text)
            except Exception:
                raise RuntimeError("oauth_details_enc could not be decrypted/parsed")

    def decrypt(self, cipher_text):
        decrypted = boto3.client("kms").decrypt(CiphertextBlob=b64decode(cipher_text))[
            "Plaintext"
        ]
        return decrypted.decode("utf-8")


CFG = Config(
    os.environ["BAG_PATHS"],
    os.environ["STORAGE_SPACE"],
    os.environ["API_URL"],
    os.environ["INGESTS_BUCKET"],
    os.environ["OAUTH_DETAILS_ENC"],
)


daiquiri.setup(level=os.environ.get("LOG_LEVEL", "INFO"))


@log_on_error
def main(event, context):
    logger = daiquiri.getLogger(__name__)
    logger.info("received trigger event")
    client = WellcomeStorageClient(CFG.api_url, CFG.oauth_details)

    for bag_path in CFG.bag_paths:
        ingest_location = client.ingest(bag_path, CFG.ingests_bucket, CFG.storage_space)
        logger.info(
            f"triggered ingest for {CFG.bag_paths!r} location: {ingest_location!r}"
        )
