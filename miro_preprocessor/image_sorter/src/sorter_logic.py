# -*- encoding: utf-8 -*-
"""
This file contains the logic for sorting images into their final destination.

It only has a single public function: ``sort_image``, which takes a Python
dictionary and returns an instance of ``Decision``.
"""

import enum


class Decision(enum.Enum):
    cold_store = 'cold_store'
    tandem_vault = 'tandem_vault'
    catalogue_api = 'catalogue_api'


class Undecidable(Exception):
    pass


def sort_image(collection, image_data):
    # For now reproduce the behaviour of the miro_adapter and send all images to the catalogue API.
    return Decision.catalogue_api

    raise Undecidable(
        f'No decision for collection={collection!r}, image_data={image_data!r}'
    )
