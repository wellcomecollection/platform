# -*- encoding: utf-8 -*-

from lxml import etree
import pytest

from utils import elem_to_dict


@pytest.mark.parametrize('xml_string, expected_data', [
    # Example with a list
    (
        '''
        <fruit>
            <_>apple</_>
            <_>banana</_>
            <_>cherry</_>
        </fruit>''',
        ['apple', 'banana', 'cherry']
    ),

    # Example with a dict
    (
        '''
        <apple>
            <color>red</color>
            <variety>cox</variety>
            <taste></taste>
        </apple>''',
        {
            'color': 'red',
            'variety': 'cox',
            'taste': None
        }
    ),

    # Nested list
    (
        '''
        <apple>
            <colors>
                <_>red</_>
                <_>green</_>
            </colors>
            <varieties>
                <_>ambrosia</_>
                <_>bramley</_>
                <_>cox</_>
            </varieties>
        </apple>''',
        {
            'colors': ['red', 'green'],
            'varieties': ['ambrosia', 'bramley', 'cox'],
        }
    ),

    # Nested dict
    (
        '''
        <apple>
            <ambrosia>
                <color>
                    <_>red</_>
                    <_>yellow</_>
                </color>
                <country>Canada</country>
            </ambrosia>
            <braeburn>
                <color>red</color>
                <country>New Zealand</country>
            </braeburn>
            <cox>
                <color>orange</color>
                <country>United Kingdom</country>
            </cox>
        </apple>''',
        {
            'ambrosia': {'color': ['red', 'yellow'], 'country': 'Canada'},
            'braeburn': {'color': 'red', 'country': 'New Zealand'},
            'cox': {'color': 'orange', 'country': 'United Kingdom'},
        }
    ),

    # Whitespace is trimmed from records
    (
        '''
        <apple>
            <variety>braeburn\n</variety>
            <color>\tred</color>
            <count>\r\r5\n</count>
        </apple>
        ''',
        {
            'variety': 'braeburn',
            'color': 'red',
            'count': '5',
        }
    ),

    # Empty or whitespace-only values are replaced with None
    (
        '''
        <apple>
            <variety>\n\n\r</variety>
            <color></color>
        </apple>
        ''',
        {
            'variety': None,
            'color': None,
        }
    ),
])
def test_elem_to_dict(xml_string, expected_data):
    """Parsing XML strings gives the correct data."""
    elem = etree.fromstring(xml_string)
    assert elem_to_dict(elem) == expected_data


@pytest.mark.parametrize('xml_string', [
    # List-like structure where not all the tags are correct
    '''
    <apple>
        <_>braeburn</_>
        <__>not an underscore</__>
    </apple>
    ''',

    # Dict-like structure with non-unique keys
    '''
    <apple>
        <variety>braeburn</variety>
        <variety>cox</variety>
    </apple>
    ''',
])
def test_bad_elem_throws_assert_error(xml_string):
    """Parsing malformed XML strings throws an AssertionError."""
    elem = etree.fromstring(xml_string)
    with pytest.raises(AssertionError):
        elem_to_dict(elem)
