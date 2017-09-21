# -*- encoding: utf-8 -*-
"""
This file contains the logic for sorting images into their final destination.

It only has a single public function: ``sort_image``, which takes a Python
dictionary and returns an instance of ``Decision``.
"""

import enum
import re


class Rules:
    def __init__(self, collection, image_data):
        self.collection = collection
        self.image_data = image_data

    @staticmethod
    def _normalise_string(s):
        if s is not None:
            s = s.lower()
        if s == "":
            s = None
        return s

    def _get(self, key):
        return self._normalise_string(self.image_data.get(key))

    def _compare(self, key, value):
        return self._get(key) == self._normalise_string(value)

    def _is_collection(self, collection_name):
        return self._normalise_string(self.collection) == f"source/images-{collection_name}"

    @property
    def is_f_collection(self):
        return self._is_collection("f")

    @property
    def is_l_or_m_or_v_collection(self):
        return self._is_collection("l") or self._is_collection("v") or self._is_collection("m")

    @property
    def image_library_dept_is_Archives_and_Manuscripts(self):
        return self._compare("image_library_dept", "Archives and Manuscripts")

    @property
    def image_tech_captured_mode_is_videodisc(self):
        return self._compare("image_tech_captured_mode", "videodisc")


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

    def _is_collection(collection_name):
        return _normalise_string(collection) == f"source/images-{collection_name}"

    title_matches = [
        _normalise_string("Awaiting description"),
        _normalise_string("Awaiting removal from MIRO as a duplicate"),
        _normalise_string("Awaiting captions and consent form"),
        _normalise_string("Awaiting catalogue details"),
        _normalise_string("Awaiting caption info"),
        _normalise_string("No info available for this object"),
        _normalise_string("No neg")
    ]

    c = {
        "All Images-F": (
            _is_collection("f")
        ),
        "Collection is L,V or M": (
            _is_collection("l") or _is_collection("v") or _is_collection("m")
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

    print(c)
    print(image_data)
    rules = Rules(collection, image_data)

    if rules.is_f_collection or \
            (rules.is_l_or_m_or_v_collection and rules.image_library_dept_is_Archives_and_Manuscripts) or \
            (rules.is_l_or_m_or_v_collection and rules.image_tech_captured_mode_is_videodisc):
        return Decision.cold_store

    raise Undecidable(
        f'No decision for collection={collection!r}, image_data={image_data!r}'
    )
