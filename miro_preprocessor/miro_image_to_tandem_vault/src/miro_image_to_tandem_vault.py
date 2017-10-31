# -*- encoding: utf-8 -*-
"""
Lambda for loading images and data into Tandem Vault
"""

import attr
import json
import os
import re

import boto3

from wellcome_lambda_utils.miro_utils import MiroImage

import tandem_vault
import tandem_vault_metadata


class InvalidWIAYear(Exception):
    pass


@attr.s
class MiroCollection:
    """
    Represents an old Miro collection, as stored in Tandem Vault.
    """
    upload_set_id = attr.ib()
    collection_id = attr.ib()


miro_collections = {
    'A': MiroCollection(47806, 111597),  # noqa
    'AS': MiroCollection(47807, 111598),  # noqa
    'B': MiroCollection(47808, 111600),  # noqa
    'D': MiroCollection(47809, 111601),  # noqa
    'F': MiroCollection(47810, 111603),  # noqa
    'FP': MiroCollection(47811, 111604),  # noqa
    'L': MiroCollection(47812, 111605),  # noqa
    'M': MiroCollection(47813, 111606),  # noqa
    'N:': MiroCollection(47814, 111607),  # noqa
    'S': MiroCollection(47815, 111608),  # noqa
    'V': MiroCollection(47816, 111609),  # noqa
    'W': MiroCollection(47817, 1116011),  # noqa
}

wia_year = {
    '1997': 110075,
    '1998': 110076,
    '1999': 110077,
    '2001': 110078,
    '2002': 110079,
    '2005': 110080,
    '2006': 110081,
    '2008': 110082,
    '2009': 110083,
    '2011': 110084,
    '2012': 110085,
    '2014': 110086,
    '2015': 110087,
    '2016': 110088,
    '2017': 110089,
}


def is_wia_award_winner(miro_image):
    if "Biomedical Image Awards" in miro_image.image_data['image_award']:
        return True

    if "Wellcome Image Awards" in miro_image.image_data['image_award']:
        return True

    return False


def determine_wia_collection(miro_image):
    if miro_image['image_award_date'] in wia_year:
        return wia_year[miro_image['image_award_date']]

    raise InvalidWIAYear(miro_image)


def determine_miro_collection(miro_image):
    return miro_collections[miro_image.collection].collection_id


def extract_image_from_event(event):
    return MiroImage(
        json.loads(event['Records'][0]['Sns']['Message'])
    )


def upload_asset(tandem_vault_api, s3_client, src_bucket, miro_image):
    src_key = f"fullsize/{miro_image.image_path}.jpg"

    image = s3_client.get_object(
        Bucket=src_bucket,
        Key=src_key
    )['Body']

    upload_set_id = miro_collections[miro_image.collection].upload_set_id

    return tandem_vault_api.upload_image_to_tv(
        image,
        src_key,
        upload_set_id
    )


def main(event, _):
    print(f'Received event:\n{event}')
    s3_client = boto3.client('s3')

    tandem_vault_api_key = os.environ['TANDEM_VAULT_API_KEY']
    tandem_vault_api_url = os.environ['TANDEM_VAULT_API_URL']
    src_bucket = os.environ['IMAGE_SRC_BUCKET']

    tandem_vault_api = tandem_vault.TandemVaultAPI(
        tandem_vault_api_url,
        tandem_vault_api_key
    )

    miro_image = extract_image_from_event(event)

    # Upload asset
    asset_data = upload_asset(
        tandem_vault_api,
        s3_client,
        src_bucket,
        miro_image
    )

    asset_id = asset_data['id']

    # Add to miro collection
    miro_collection_id = determine_miro_collection(miro_image)
    tandem_vault_api.add_image_to_collection(asset_id, miro_collection_id)

    # Add to wia collection
    if is_wia_award_winner(miro_image):
        wia_collection_id = determine_wia_collection(miro_image)
        tandem_vault_api.add_image_to_collection(asset_id, wia_collection_id)

    # Add metadata
    metadata = tandem_vault_metadata.create_metadata(miro_image.image_data)
    tandem_vault_api.add_image_metadata(asset_id, metadata)

    # Add tags
    tags = tandem_vault_metadata.create_tags(miro_image.image_data)
    tandem_vault_api.add_image_tags(asset_id, tags)
