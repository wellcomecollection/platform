# -*- encoding: utf-8 -*-
"""
Elasticsearch mapping for Collection records in Calm.
"""

import json

import requests


MAPPING = {
    'properties': {
        'AccNo': {
            'type': 'string',
            'index': 'not_analyzed',
        },
        'AltRefNo': {
            'type': 'string',
            'index': 'not_analyzed',
        },
        'Date': {
            'properties': {
                'raw': {
                    'type': 'string',
                    'index': 'not_analyzed',
                },
                'start': {
                    'type': 'date',
                },
                'end': {
                    'type': 'date',
                },
            },
        },
        'Description': {
            'type': 'string',
        },
        'Language': {
            'type': 'string',
        },
        'Notes': {
            'type': 'string',
        },
        'Title': {
            'type': 'string',
        },
    }
}


def push_mapping(es_host, index):
    r = requests.put(
        '%s/%s/_mapping/collection' % (es_host, index),
        data=json.dumps(MAPPING))
    print(r, r.text)
    assert r.status_code == 200


if __name__ == '__main__':
    push_mapping('http://localhost:9200', 'calm_index')
