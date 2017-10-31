# -*- encoding: utf-8 -*-


import tandem_vault_metadata

image_data = {
    'image_no_calc': 'image_no_calc',
    'image_award': 'image_award',
    #Caption
    'image_no_number': "image_no_number",
    'image_title': "image_title",
    'image_innopac_id': "image_innopac_id",
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
    'image_related_images': "image_related_images",
    #Usage
    'image_use_restrictions': 'image_use_restrictions',
    'image_copyright_cleared': 'Y',
    'image_general_use': 'N',
    #Creator
    'image_creator': ["Bob Snappypic", "Jim Photoguy"],
    #Credit
    'image_credit_line': 'Wellcome Library',
    #Tags
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

expected_caption_text = \
    "image_no_number\n" + \
    "image_title, Sierra record number: image_innopac_id\n" + \
    "image_pub_author, Bob Snappypic/Jim Photoguy, image_pub_title, image_pub_publisher, image_pub_date\n" + \
    "image_pub_periodical, image_pub_volume, image_pub_issue, image_pub_page_no, image_pub_plate\n" + \
    "image_image_desc\n" + \
    "image_library_ref_department\n" + \
    "image_pub_archive\n" + \
    "image_library_dept\n" + \
    "Used for exhibition: image_award\n" + \
    "Used for Wellcome publication: image_wellcome_publications\n" + \
    "Photographer: image_tech_scanned_by\n" + \
    "Transparency held: Yes\n" + \
    "image_related_images"

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


def test_create_usage():
    actual_text = tandem_vault_metadata.create_usage(image_data)
    assert actual_text == expected_usage_text


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