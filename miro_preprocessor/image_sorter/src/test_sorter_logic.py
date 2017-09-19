# -*- encoding: utf-8 -*-

import pytest

from sorter_logic import Decision, Undecidable, sort_image


@pytest.mark.parametrize('collection, image_data', [
])
def test_is_undecidable(collection, image_data):
    """These examples are undecidable."""
    with pytest.raises(Undecidable):
        sort_image(image_data)


@pytest.mark.parametrize('collection, image_data', [
    # TODO: Write some examples...
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
