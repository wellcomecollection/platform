# -*- encoding: utf-8 -*-

import pytest

from sorter_logic import Decision, Undecidable, sort_image


def collection_image_data(**kwargs):
    image = {
        'collection': 'source/images-A',
        'image_data': {
            "image_title": "Image Title",
            "image_pub_title": "Image Pub Title",
            "image_pub_periodical": "Lost socks monthly",
            "image_library_dept": "Paperclips and hairnets",
            "image_tech_captured_mode": "Frog retina",
            "image_copyright_cleared": "N",
            "image_access_restrictions": "CC-BY",
            "image_general_use": "N",
            "image_innopac_id": "12345678"
        }}
    collection = image['collection']
    if 'collection' in kwargs.keys():
        collection = kwargs.pop('collection')
    image_data = image['image_data']
    image_data.update(kwargs)

    return collection, image_data


@pytest.mark.parametrize('collection, image_data', [
    # TODO: Write some examples...
])
def test_is_undecidable(collection, image_data):
    """These examples are undecidable."""
    with pytest.raises(Undecidable):
        sort_image(collection, image_data)


@pytest.mark.parametrize('collection, image_data', [
    collection_image_data(collection='source/images-F'),
    collection_image_data(collection='source/images-L', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='source/images-V', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='source/images-M', image_library_dept="Archives and Manuscripts"),
    collection_image_data(collection='source/images-L', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='source/images-V', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='source/images-M', image_tech_captured_mode="videodisc"),
    collection_image_data(collection='source/images-L',
                          image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="1234567"),
    collection_image_data(collection='source/images-V',
                          image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="1234567"),
    collection_image_data(collection='source/images-M',
                          image_title=None,
                          image_pub_title=None,
                          image_pub_periodical=None,
                          image_innopac_id="1234567"),
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
