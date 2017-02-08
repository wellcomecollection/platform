#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
Function for tidying the 'Language' field in Calm.

The values in the 'Language' field are:
1. A mishmash of different formats and values
2. A string

In Elasticsearch, it would be better if the language field was a list
of strings: one per language.  This file contains a function that transforms
the Calm values into such a list, along with accompanying tests.

"""

import html
import re

import pytest


def tidy_calm_language_value(calm_value):
    """Given a 'Language' value from Calm, return a list of languages."""
    if calm_value is None:
        return []

    # Discard trailing/leading characters that aren't part of a language name
    calm_value = calm_value.strip().strip('.`')
    if not calm_value:
        return []

    # Special cases, based on real data from the Calm database
    if calm_value.lower() == 'eng':
        return ['English']

    # Get rid of HTML entities, then throw away anything in <language> tags.
    # This has some weird messy stuff that isn't human-readable.
    calm_value = html.unescape(calm_value)
    calm_value = re.sub(r'</?language[^>]*/?>', '', calm_value)

    calm_value = (
        calm_value.replace(' and ', ',')
                  .replace('/', ',')
                  .replace(';', ',')
    )

    # TODO: What about resources where we have 'mainly/smaller'.  Should we
    # emphasise that here in the ordr?
    return sorted([
        lang.strip()
        for lang in calm_value.split(',')
        if lang.strip()
    ])


@pytest.mark.parametrize('calm_value, es_value', [
    (None,                          []),
    (' ',                           []),
    ('English',                     ['English']),
    ('French',                      ['French']),
    ('English ',                    ['English']),
    ('English.',                    ['English']),
    ('eng',                         ['English']),
    ('English and Chinese',         ['English', 'Chinese']),
    ('English, German and French',  ['English', 'German', 'French']),
])
def test_language_tidier(calm_value, es_value):
    assert tidy_calm_language_value(calm_value) == es_value
