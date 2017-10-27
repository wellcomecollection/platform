# -*- encoding: utf-8 -*-
"""
Lambda for loading images and data into Tandem Vault
"""

import attr
import json
import os
import re

import tandem_vault


@attr.s
class MiroCollection:
    """
    Represents an old Miro collection, as stored in Tandem Vault.
    """
    upload_set_id = attr.ib()
    collection_id = attr.ib()


# These collections were created partially using the code below, partially
# by hand.  Because they're a fixed set, we just hard-code the details here,
# rather than trying to derive them programatically from the API.
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


def _miro_prefix(s3_key):
    """
    Given an S3 key (e.g. 'B0007000/B00070001.jpg'), return the Miro prefix.
    """
    filename = os.path.basename(s3_key)
    return re.search(r'^[A-Z]+', filename).group(0)


def _upload_to_tv(tandem_vault_api, src_key):
    prefix = _miro_prefix(src_key)
    upload_set_id = miro_collections[prefix].upload_set_id

    tandem_vault_api.upload_image_to_tv(src_key, upload_set_id)


def _add_image_to_collection(tandem_vault_api, asset_data):
    prefix = _miro_prefix(asset_data['filename'])
    collection_id = miro_collections[prefix].collection_id
    asset_id = asset_data['id']

    tandem_vault_api.add_image_to_collection(asset_id, collection_id)


def main(event, _):
    print(f'Received event:\n{event}')

    tandem_vault_api_key = os.environ['TANDEM_VAULT_API_KEY']
    tandem_vault_api_url = os.environ['TANDEM_VAULT_API_URL']

    tandem_vault_api = tandem_vault.TandemVaultAPI(tandem_vault_api_url, tandem_vault_api_key)
    image_info = json.loads(event['Records'][0]['Sns']['Message'])
