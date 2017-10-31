# -*- encoding: utf-8 -*-
"""
Module for creating metadata to feed Tandem Vault from Miro
"""


def _if_exists(s, prepend = ""):
    if not s:
        return ""

    return f"{prepend}{s}"


def _followed_by_comma(s, prepend = ""):
    if not s:
        return ""

    return f"{prepend}{s}, "


def _followed_by_newline(s, prepend = ""):
    if not s:
        return ""

    return f"{prepend}{s}\n"


def _show_only_if_match_hide_value(s, match, text):
    if s != match:
        return ""

    return f"{text}\n"


def _show_only_if_match(s, match, prepend = ""):
    if s != match:
        return ""

    return _followed_by_newline(s, prepend)


def _or(s1,s2):
    if s1:
        return s1

    return s2


def create_usage(d):
    parts = [
        _followed_by_newline(d['image_use_restrictions']),
        _followed_by_newline(d['image_copyright_cleared'], "Likely in copyright. Cleared for open access? "),
        _show_only_if_match_hide_value(
            d['image_general_use'],
            'N',
            'May be sensitive or unsuitable for general use.'
        ),
    ]

    return "".join(parts)


def create_caption(d):
    parts = [
        _followed_by_newline(d['image_no_number']),
        _followed_by_comma(d['image_title']),
        _followed_by_newline(d['image_innopac_id'], 'Sierra record number: '),
        _followed_by_comma(d['image_pub_author']),
        _followed_by_comma(create_creator(d)),
        _followed_by_comma(d['image_pub_title']),
        _followed_by_comma(d['image_pub_publisher']),
        _or(
            d['image_pub_date'],
            d['image_artwork_date'],
        ),
        '\n',
        _followed_by_comma(d['image_pub_periodical']),
        _followed_by_comma(d['image_pub_volume']),
        _followed_by_comma(d['image_pub_issue']),
        _followed_by_comma(d['image_pub_page_no']),
        _followed_by_newline(d['image_pub_plate']),
        _followed_by_newline(d['image_image_desc']) + \
        _followed_by_newline(d['image_library_ref_department']) + \
        _followed_by_newline(d['image_pub_archive']) + \
        _followed_by_newline(d['image_library_dept']) + \
        _followed_by_newline(d['image_award'], "Used for exhibition: ") + \
        _followed_by_newline(d['image_wellcome_publications'], "Used for Wellcome publication: ") + \
        _followed_by_newline(d['image_tech_scanned_by'], "Photographer: ") + \
        _show_only_if_match(d['image_transparency_held'], 'Yes', 'Transparency held: '),
        d['image_related_images'],
        ]

    return "".join(parts)


def create_copyright(d):
    if 'Wellcome' in d['image_credit_line']:
        return 'Wellcome Collection'

    return d['image_credit_line']


def create_notes():
    return "Important usage information:\n" + \
           "Some images are in copyright or unsuitable for general use due to sensitivities. " + \
           "Please refer to the image metadata before sharing. " + \
           "If you are unable to download an image that you need, " + \
           "or if you are unsure about what use is permitted, " + \
           "please contact digitisation@wellcome.ac.uk."


def create_tags(image_data):
    def _split(s):
        tokens = s.lower().split(',')

        return [t.strip() for t in tokens]

    return sorted(list(set(
        _split(image_data['image_subject_names']) + \
        _split(image_data['image_keywords']) + \
        _split(image_data['image_lcsh_place']) + \
        _split(image_data['image_mesh']) + \
        _split(image_data['image_loc'])
    )))


def create_creator(image_data):
    creators = image_data['image_creator']

    if not isinstance(creators, list):
        creators = [creators]

    return "/".join(creators)


def create_metadata(image_data):
    caption = create_caption(image_data)
    usage = create_usage(image_data)
    creator = create_creator(image_data)
    copyright = create_copyright(image_data)
    notes = create_notes()

    return {
        "caption": caption,
        "usage": usage,
        "creator": creator,
        "copyright": copyright,
        "notes": notes
    }
