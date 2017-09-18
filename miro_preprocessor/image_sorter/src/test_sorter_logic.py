# -*- encoding: utf-8 -*-

import pytest

from sorter_logic import Decision, Undecidable, sort_image


@pytest.mark.parametrize('metadata', [
    {},
])
def test_is_undecidable(metadata):
    """These examples are undecidable."""
    with pytest.raises(Undecidable):
        sort_image(metadata)


@pytest.mark.parametrize('metadata', [
    # TODO: Write some examples...
])
def test_is_cold_store(metadata):
    """These examples all end up in cold store."""
    assert sort_image(metadata) == Decision.cold_store


@pytest.mark.parametrize('metadata', [
    # TODO: Write some examples...
])
def test_is_tandem_vault(metadata):
    """These examples all end up in Tandem Vault."""
    assert sort_image(metadata) == Decision.tandem_vault


@pytest.mark.parametrize('metadata', [
    # TODO: Write some examples...
])
def test_is_digital_library(metadata):
    """These examples all end up in the Digital Library."""
    assert sort_image(metadata) == Decision.digital_library
