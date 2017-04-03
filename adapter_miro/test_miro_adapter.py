# -*- encoding: utf-8 -*-

from lxml import etree
import pytest

from utils import elem_to_dict


@pytest.mark.parametrize('xml_string, expected_dict', [
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
])
def test_elem_to_dict(xml_string, expected_dict):
    elem = etree.fromstring(xml_string)
    assert elem_to_dict(elem) == expected_dict
