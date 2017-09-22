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

    _empty_title_strings = [
        "-",
        "--",
        "Awaiting description",
        "Awaiting removal from MIRO as duplicate",
        "Awaiting captions and consent form",
        "Awaiting catalogue details",
        "Awaiting caption info",
        "No info available about this object",
        "No neg"
    ]

    @staticmethod
    def _normalise_string(s):
        if s is not None:
            s = s.lower().strip()
        if s == "":
            s = None
        return s

    def _get(self, key):
        return self._normalise_string(self.image_data.get(key))

    def _compare(self, key, value):
        return self._get(key) == self._normalise_string(value)

    def _is_collection(self, collection_name):
        return self._normalise_string(self.collection) == self._normalise_string(f"source/images-{collection_name}")

    def _search(self, regex, key):
        return re.search(regex, self._get(key))

    def _is_blank(self, key):
        return self._get(key) is None

    def _key_matches(self, key, string_list):
        return any([self._compare(key, string) for string in string_list])

    @property
    def is_f_collection(self):
        return self._is_collection("F")

    @property
    def is_l_or_m_or_v_collection(self):
        return self._is_collection("L") or self._is_collection("V") or self._is_collection("M")

    @property
    def image_library_dept_is_Archives_and_Manuscripts(self):
        return self._compare("image_library_dept", "Archives and Manuscripts")

    @property
    def image_tech_captured_mode_is_videodisc(self):
        return self._compare("image_tech_captured_mode", "videodisc")

    @property
    def is_innopac_id_8_digits(self):
        return self._search(r"[0-9]{7}[0-9|x]{1}", "image_innopac_id") is not None

    @property
    def is_title_blank(self):
        return self._is_blank("image_title")

    @property
    def is_title_empty(self):
        return self.is_title_blank or self._key_matches('image_title', self._empty_title_strings)

    @property
    def is_image_pub_title_blank(self):
        return self._is_blank("image_pub_title")

    @property
    def is_image_pub_periodical_blank(self):
        return self._is_blank("image_pub_periodical")


class Decision(enum.Enum):
    cold_store = 'cold_store'
    tandem_vault = 'tandem_vault'
    catalogue_api = 'catalogue_api'


def sort_image(collection, image_data):

    print(image_data)
    r = Rules(collection, image_data)

    if r.is_f_collection or \
            (r.is_l_or_m_or_v_collection and r.image_library_dept_is_Archives_and_Manuscripts) or \
            (r.is_l_or_m_or_v_collection and r.image_tech_captured_mode_is_videodisc) or \
            (r.is_l_or_m_or_v_collection and not r.is_innopac_id_8_digits and r.is_title_empty and r.is_image_pub_title_blank and r.is_image_pub_periodical_blank):
        return Decision.cold_store
    else:
        return Decision.catalogue_api
