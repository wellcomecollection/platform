# -*- encoding: utf-8 -*-
"""
Module for creating metadata to feed Tandem Vault from Miro
"""

import json

import attr
import boto3


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
    'B': MiroCollection(49302, 111600),  # noqa
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


class InvalidWIAYear(Exception):
    pass


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


CONTRIB_MAP = None


def _contrib_map(bucket='miro-data', key='contrib_map.json'):
    global CONTRIB_MAP
    if CONTRIB_MAP is None:
        s3 = boto3.resource('s3')

        obj = s3.Object(bucket, key)
        object_body = obj.get()['Body'].read().decode('utf-8')

        CONTRIB_MAP = json.loads(object_body)

    return CONTRIB_MAP


def lookup_contributor(d):
    contrib_map = _contrib_map()

    if 'image_source_code' not in d:
        return ""

    contrib_code = d['image_source_code']

    if contrib_code not in contrib_map:
        return ""

    contributor = contrib_map[contrib_code]
    if not contributor:
        return ""

    return contributor


def _is_in(d, a):
    if a not in d:
        return ""
    else:
        return d[a] or ""


def _if_exists(d, a, prepend=""):
    s = _is_in(d, a)

    if not s:
        return ""

    return f"{prepend}{s}"


def _followed_by_comma(d, a, prepend=""):
    s = _is_in(d, a)

    if not s:
        return ""

    return f"{prepend}{s}, "


def _followed_by_newline(d, a, prepend="", delimiter=", "):
    s = _is_in(d, a)

    if isinstance(s, list):
        s = delimiter.join(s)

    if not s:
        return ""

    return f"{prepend}{s}\n"


def _show_only_if_match_hide_value(d, a, match, text):
    s = _is_in(d, a)

    if s != match:
        return ""

    return f"{text}\n"


def _show_only_if_match(d, a, match, prepend=""):
    s = _is_in(d, a)

    if s != match:
        return ""

    return _followed_by_newline(d, a, prepend)


def _or(s1, s2):
    if s1:
        return s1

    if not s2:
        return ""

    return s2


def create_usage(d):
    parts = [
        _followed_by_newline(d, 'image_use_restrictions'),
        _followed_by_newline(d, 'image_copyright_cleared', "Likely in copyright. Cleared for open access? "),
        _show_only_if_match_hide_value(
            d, 'image_general_use', 'N',
            'May be sensitive or unsuitable for general use.'
        ),
    ]

    return "".join(parts)


def create_caption(d):
    parts = [
        _followed_by_newline(d, 'image_no_calc'),
        _followed_by_comma(d, 'image_title'),
        _followed_by_newline(d, 'image_innopac_id', 'Sierra record number: '),
        _followed_by_comma(d, 'image_pub_author'),
        _followed_by_comma({'creator': create_creator(d)}, 'creator'),
        _followed_by_comma(d, 'image_pub_title'),
        _followed_by_comma(d, 'image_pub_publisher'),
        _or(
            _is_in(d, 'image_pub_date'),
            _is_in(d, 'image_artwork_date'),
        ),
        '\n',
        _followed_by_comma(d, 'image_pub_periodical'),
        _followed_by_comma(d, 'image_pub_volume'),
        _followed_by_comma(d, 'image_pub_issue'),
        _followed_by_comma(d, 'image_pub_page_no'),
        _followed_by_newline(d, 'image_pub_plate'),
        _followed_by_newline(d, 'image_image_desc') +
        _followed_by_newline(d, 'image_library_ref_department') +
        _followed_by_newline(d, 'image_pub_archive') +
        _followed_by_newline(d, 'image_library_dept') +
        _followed_by_newline(d, 'image_award', "Used for exhibition: ") +
        _followed_by_newline(d, 'image_wellcome_publications', "Used for Wellcome publication: ") +
        _followed_by_newline(d, 'image_tech_scanned_by', "Photographer: ") +
        _show_only_if_match(d, 'image_transparency_held', 'Yes', 'Transparency held: '),
        _is_in(d, 'image_related_images'),
    ]

    print(parts)

    return "".join(parts)


def create_copyright(d):
    image_credit_line = _is_in(d, 'image_credit_line')

    if 'Wellcome' in image_credit_line:
        return 'Wellcome Collection'

    if not image_credit_line:
        possible_creator = lookup_contributor(d)
        if possible_creator:
            return possible_creator

    return ""


def create_notes():
    return "".join([
        "Important usage information:\n",
        "Some images are in copyright or unsuitable for general use due to sensitivities. ",
        "Please refer to the image metadata before sharing. ",
        "If you are unable to download an image that you need, ",
        "or if you are unsure about what use is permitted, ",
        "please contact digitisation@wellcome.ac.uk."
    ])


def create_tags(d):
    def _split(s):
        if not isinstance(s, list):
            tokens = s.lower().split(',')
        else:
            tokens = [t.lower() for t in s]

        return [t.strip() for t in tokens]

    return sorted(list(set(
        _split(_is_in(d, 'image_subject_names')) +
        _split(_is_in(d, 'image_keywords')) +
        _split(_is_in(d, 'image_lcsh_place')) +
        _split(_is_in(d, 'image_mesh')) +
        _split(_is_in(d, 'image_loc'))
    )))


def create_creator(d):
    creators = _is_in(d, 'image_creator')

    if not creators:
        possible_creator = lookup_contributor(d)
        if possible_creator:
            return possible_creator

    if not isinstance(creators, list):
        creators = [creators]

    return "/".join(creators)


def create_metadata(image_data):
    caption = create_caption(image_data)
    usage = create_usage(image_data)
    creator = create_creator(image_data)
    copy_right = create_copyright(image_data)
    notes = create_notes()

    return {
        "caption": caption,
        "usage": usage,
        "creator": creator,
        "copyright": copy_right,
        "notes": notes
    }
