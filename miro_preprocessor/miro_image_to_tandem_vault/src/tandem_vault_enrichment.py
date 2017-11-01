#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Lambda for loading images and data into Tandem Vault
"""

import json
import os

import boto3

from wellcome_lambda_utils.miro_utils import MiroImage

from tandem_vault_metadata import (
    miro_collections,
    create_metadata,
    create_tags,
    InvalidWIAYear,
    wia_year
)

import tandem_vault_api
import sqs_utils


def is_wia_award_winner(miro_image):
    if "Biomedical Image Awards" in miro_image.image_data['image_award']:
        return True

    if "Wellcome Image Awards" in miro_image.image_data['image_award']:
        return True

    return False


def determine_wia_collection(image_data):
    assert len(image_data['image_award_date']) == 1

    if image_data['image_award_date'][0] in wia_year:
        return wia_year[image_data['image_award_date'][0]]

    raise InvalidWIAYear(image_data)


def main():
    sqs_client = boto3.client('sqs')

    queue_url = os.environ['QUEUE_URL']

    tandem_vault_api_key = os.environ['TANDEM_VAULT_API_KEY']
    tandem_vault_api_url = os.environ['TANDEM_VAULT_API_URL']

    api = tandem_vault_api.TandemVaultAPI(
        tandem_vault_api_url,
        tandem_vault_api_key
    )

    sqs_reader = sqs_utils.SQSReader(sqs_client, queue_url)

    for message in sqs_reader:
        outer_message = json.loads(message['Body'])
        tandem_vault_upload_info = json.loads(outer_message['Message'])

        miro_image = MiroImage(tandem_vault_upload_info['image_info'])
        asset_id = tandem_vault_upload_info['asset_id']

        # Add to miro collection
        miro_collection_id = miro_collections[miro_image.collection].collection_id
        api.add_image_to_collection(asset_id, miro_collection_id)

        # Add to wia collection
        if is_wia_award_winner(miro_image):
            wia_collection_id = determine_wia_collection(miro_image.image_data)
            api.add_image_to_collection(asset_id, wia_collection_id)

        # Add metadata
        metadata = create_metadata(miro_image.image_data)
        api.add_image_metadata(asset_id, metadata)

        # Add tags
        tags = create_tags(miro_image.image_data)
        api.add_image_tags(asset_id, tags)

        sqs_reader.delete_current()


if __name__ == '__main__':
    main()