# -*- encoding: utf-8 -*-
"""
This file contains the logic for sorting images into their final destination.

It only has a single public function: ``sort_image``, which takes a Python
dictionary and returns an instance of ``Decision``.
"""

import enum
import re


class Decision(enum.Enum):
    cold_store = 'cold_store'
    tandem_vault = 'tandem_vault'
    catalogue_api = 'catalogue_api'


class Undecidable(Exception):
    pass


def sort_image(collection, image_data):
    def _normalise_string(s):
        if s is not None:
            s = s.lower()
        if s == "":
            s = None
        return s

    def _get(key):
        return _normalise_string(image_data.get(key))

    def _compare(key, value):
        return _get(key) == _normalise_string(value)

    def _search(regex, key):
        return re.search(regex, _get(key))

    def _is_blank(key):
        return _get(key) is None

    title_matches = [
        _normalise_string("Awaiting description"),
        _normalise_string("Awaiting removal from MIRO as a duplicate"),
        _normalise_string("Awaiting captions and consent form"),
        _normalise_string("Awaiting catalogue details"),
        _normalise_string("Awaiting caption info"),
        _normalise_string("No info available for this object"),
        _normalise_string("No neg")
    ]

    checks = {
        "All Images-F": (
            _normalise_string(collection) == "images-f"
        ),
        "image_library_dept is Archives and Manuscripts": (
            _compare("image_library_dept", "Archives and Manuscripts")
        ),
        "image_tech_captured_mode is videodisc": (
            _compare("image_tech_captured_mode", "videodisc")
        ),
        "image_innopac_id does not contain an 8 digit number": (
            _search("[0-9]{8}", "image_innopac_id") is None
        ),
        "image_title is blank": (
            _is_blank("image_title")
        ),
        "image_pub_title is blank": (
            _is_blank("image_pub_title")
        ),
        "image_pub_periodical is blank": (
            _is_blank("image_pub_periodical")
        ),
        "image_title is not one of title_matches": (
            any(
                [_compare("image_title", s) for s in title_matches]
            ) or _search("-{1,2}", "image_title")
        ),
    }

    print(checks)
    print(image_data)

    if any(checks.values()):
        return Decision.cold_store

    raise Undecidable(
        f'No decision for collection={collection!r}, image_data={image_data!r}'
    )
