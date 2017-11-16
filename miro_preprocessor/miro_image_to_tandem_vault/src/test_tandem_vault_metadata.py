# -*- encoding: utf-8 -*-

import pytest

import tandem_vault_metadata

image_data = {
    # Caption
    'image_no_calc': 'image_no_calc',
    'image_title': "image_title",
    'image_award': 'image_award',
    'image_innopac_id': "<image_innopac_id>",
    'image_pub_author': "image_pub_author",
    'image_pub_title': "image_pub_title",
    'image_pub_publisher': "image_pub_publisher",
    'image_pub_date': "image_pub_date",
    'image_artwork_date': "image_artwork_date",
    'image_pub_periodical': "image_pub_periodical",
    'image_pub_volume': "image_pub_volume",
    'image_pub_issue': "image_pub_issue",
    'image_pub_page_no': "image_pub_page_no",
    'image_pub_plate': "image_pub_plate",
    'image_image_desc': "image_image_desc",
    'image_library_ref_department': "image_library_ref_department",
    'image_pub_archive': "image_pub_archive",
    'image_library_dept': "image_library_dept",
    'image_wellcome_publications': "image_wellcome_publications",
    'image_tech_scanned_by': "image_tech_scanned_by",
    'image_transparency_held': "Yes",
    'image_related_images': "<image_related_images>",
    # Usage
    'image_use_restrictions': 'image_use_restrictions',
    'image_copyright_cleared': 'Y',
    'image_general_use': 'N',
    # Creator
    'image_creator': ["Bob Snappypic", "Jim Photoguy"],
    # Credit
    'image_credit_line': 'Wellcome Library',
    # Tags
    'image_subject_names': 'image_subject_names, WORD WORD, word-word',
    'image_keywords': 'image_keywords, Word, WORD',
    'image_lcsh_place': 'image_lcsh_place,  WORD',
    'image_mesh': 'image_mesh,WORD,WORD',
    'image_loc': 'image_loc',
}

expected_usage_text = \
    "image_use_restrictions\n" + \
    "Likely in copyright. Cleared for open access? Y\n" + \
    "May be sensitive or unsuitable for general use.\n"

expected_caption_text = '\n'.join([
    "image_no_calc",
    "image_title",
    "Sierra record number: b<image_innopac_id>",
    "image_pub_author, Bob Snappypic/Jim Photoguy, image_pub_title, image_pub_publisher, image_pub_date",
    "image_pub_periodical, image_pub_volume, image_pub_issue, image_pub_page_no, image_pub_plate",
    "image_image_desc",
    "image_pub_archive",
    "image_library_dept",
    "Used for exhibition: image_award",
    "Used for Wellcome publication: image_wellcome_publications",
    "Photographer: image_tech_scanned_by",
    "Transparency held: Yes",
    "Related images: <image_related_images>",
])

expected_creator_text = "Bob Snappypic/Jim Photoguy"

expected_copyright_text = "Wellcome Collection"

expected_notes_text = \
    "Important usage information:\n" + \
    "Some images are in copyright or unsuitable for general use due to sensitivities. " + \
    "Please refer to the image metadata before sharing. " + \
    "If you are unable to download an image that you need, " + \
    "or if you are unsure about what use is permitted, " + \
    "please contact digitisation@wellcome.ac.uk."

expected_tags = [
    "image_keywords",
    "image_lcsh_place",
    "image_loc",
    "image_mesh",
    "image_subject_names",
    "word",
    "word word",
    "word-word"
]


def test_create_caption():
    actual_text = tandem_vault_metadata.create_caption(image_data)
    assert actual_text == expected_caption_text


def test_create_creator():
    actual_text = tandem_vault_metadata.create_creator(image_data)
    assert actual_text == expected_creator_text


def test_create_copyright():
    actual_text = tandem_vault_metadata.create_copyright(image_data)
    assert actual_text == expected_copyright_text


def test_create_notes():
    actual_text = tandem_vault_metadata.create_notes()
    assert actual_text == expected_notes_text


def test_create_tags():
    actual_tags = tandem_vault_metadata.create_tags(image_data)
    assert actual_tags == expected_tags


def test_create_metadata():
    actual_metadata = tandem_vault_metadata.create_metadata(image_data)

    assert actual_metadata == {
        "caption": expected_caption_text,
        "usage": expected_usage_text,
        "creator": expected_creator_text,
        "copyright": expected_copyright_text,
        "notes": expected_notes_text
    }


@pytest.mark.parametrize('metadata, expected_contributor', [
    ({}, ''),
    ({'image_source_code': 'doesnotexist'}, ''),
    ({'image_source_code': 'ABC'}, ''),
    ({'image_source_code': 'WEL'}, 'Wellcome Collection'),
    ({'image_source_code': 'wel'}, 'Wellcome Collection'),
])
def test_lookup_contributor(metadata, expected_contributor):
    actual_contributor = tandem_vault_metadata.lookup_contributor(
        metadata, contrib_map={'WEL': 'Wellcome Collection'}
    )
    assert actual_contributor == expected_contributor


@pytest.mark.parametrize('metadata, expected_usage', [
    (image_data, expected_usage_text),
    ({}, ''),
    ({'image_use_restrictions': 'Can only be used on Tuesdays.'},
     'Can only be used on Tuesdays.\n'),
    ({'image_copyright_cleared': 'Y'},
     'Likely in copyright. Cleared for open access? Y\n'),
    ({'image_use_restrictions': 'Must be displayed upside down',
      'image_copyright_cleared': 'Y'},
     'Must be displayed upside down\n'
     'Likely in copyright. Cleared for open access? Y\n'),
    ({'image_general_use': 'N'},
     'May be sensitive or unsuitable for general use.\n'),
    ({'image_copyright_info': 'Copyright (C) Henry Wellcome',
      'image_general_use': 'N'},
     'This image has additional copyright information. '
     'Please contact digitisation@wellcome.ac.uk.\n\n'
     'May be sensitive or unsuitable for general use.\n'),
])
def test_create_usage(metadata, expected_usage):
    assert tandem_vault_metadata.create_usage(metadata) == expected_usage
