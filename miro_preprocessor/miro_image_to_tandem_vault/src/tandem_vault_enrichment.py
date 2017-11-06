#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Lambda for loading images and data into Tandem Vault
"""

import json
import logging
import os
import time

import boto3
import daiquiri

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


daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


def is_wia_award_winner(miro_image):
    if "image_award" not in miro_image.image_data:
        return False
    if not miro_image.image_data['image_award']:
        return False
    if "Biomedical Image Awards" in miro_image.image_data['image_award']:
        return True
    if "Wellcome Image Awards" in miro_image.image_data['image_award']:
        return True

    return False


def determine_wia_collection(image_data):
    if "image_award_date" not in image_data:
        raise InvalidWIAYear(image_data)
    if not image_data['image_award_date']:
        raise InvalidWIAYear(image_data)

    if not isinstance(image_data['image_award_date'], list):
        award_years = [image_data['image_award_date']]
    else:
        award_years = image_data['image_award_date']

    award_years = list(set(award_years))
    award_years = [award_year for award_year in award_years if award_year]

    if not len(award_years) == 1:
        raise InvalidWIAYear(image_data)

    if award_years[0] in wia_year:
        return wia_year[award_years[0]]

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
        try:
            time.sleep(5)
            outer_message = json.loads(message['Body'])
            tandem_vault_upload_info = json.loads(outer_message['Message'])

            miro_image = MiroImage(tandem_vault_upload_info['image_info'])
            asset_id = tandem_vault_upload_info['asset_id']

            logger.info(f"Adding metadata to {miro_image.miro_id}")
            # Add to miro collection
            miro_collection_id = miro_collections[miro_image.collection].collection_id
            api.add_image_to_collection(asset_id, miro_collection_id)

            # Add metadata
            metadata = create_metadata(miro_image.image_data)
            api.add_image_metadata(asset_id, metadata)

            # Add tags
            tags = create_tags(miro_image.image_data)
            api.add_image_tags(asset_id, tags)

            # Add to wia collection
            if is_wia_award_winner(miro_image):
                wia_collection_id = determine_wia_collection(miro_image.image_data)
                api.add_image_to_collection(asset_id, wia_collection_id)

            logger.info(f"Successfully added metadata for {miro_image.miro_id}")
        except Exception:
            logger.exception(f"Failed adding metadata for {miro_image}")
        else:
            sqs_reader.delete_current()


if __name__ == '__main__':
    main()
