# -*- encoding: utf-8 -*-

import pytest

from sorter_logic import Decision, Undecidable, sort_image


@pytest.mark.parametrize('collection, image_data', [
    # TODO: Write some examples...
])
def test_is_undecidable(collection, image_data):
    """These examples are undecidable."""
    with pytest.raises(Undecidable):
        sort_image(collection, image_data)


@pytest.mark.parametrize('collection, image_data', [
    ('Images-F', {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Archives and Manuscripts",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    })
    ,
    ('Images-A', {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Videodisc",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Image Title",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "1a"
    }),
    ('Images-A', {
        "image_title": "",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "1a"
    }),
    ('Images-A', {
        "image_title": "-",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "--",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Awaiting description",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Awaiting removal from MIRO as a duplicate",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Awaiting captions and consent form",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Awaiting catalogue details",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "Awaiting caption info",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "No info available for this object",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
    ('Images-A', {
        "image_title": "No neg",
        "image_pub_title": "Image Pub Title",
        "image_pub_periodical": "Lost socks monthly",
        "image_library_dept": "Paperclips and hairnets",
        "image_tech_captured_mode": "Frog retina",
        "image_copyright_cleared": "N",
        "image_access_restrictions": "CC-BY",
        "image_general_use": "N",
        "image_innopac_id": "12345678"
    }),
])
def test_is_cold_store(collection, image_data):
    """These examples all end up in cold store."""
    assert sort_image(collection, image_data) == Decision.cold_store


@pytest.mark.parametrize('collection, image_data', [
    # TODO: Write some examples...
])
def test_is_tandem_vault(collection, image_data):
    """These examples all end up in Tandem Vault."""
    assert sort_image(collection, image_data) == Decision.tandem_vault


@pytest.mark.parametrize('collection, image_data', [
    # TODO: Write some examples...
])
def test_is_digital_library(collection, image_data):
    """These examples all end up in the Digital Library."""
    assert sort_image(collection, image_data) == Decision.digital_library
