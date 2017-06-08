#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Standalone script for downloading the OAI-PMH for Calm.

The final output is dumped into a JSON file ``calm_records.json``, which
can be useful for doing bulk analysis of the Calm data.
"""

import collections
import json
import re
from urllib.parse import unquote

import requests


OAI_URL = 'http://archives.wellcomelibrary.org/oai/OAI.aspx'

RESUMPTION_TOKEN_RE = re.compile(
    r'<resumptionToken[^>]*>(?P<token>[^<]+)</resumptionToken>'
)

STREAM_PARSER_RE = re.compile(
    r'<(?P<name>[A-Za-z0-9]+) urlencoded=\"(?P<value>[^\"]*)\"/?>'
)


def fetch_calm_records():
    params = {
        'verb': 'ListRecords',
        'metadataPrefix': 'calm_xml'
    }
    while True:
        r = requests.get(OAI_URL, params=params)

        records = r.text.split('</record>')
        records.pop()
        for q in records:
            d = collections.defaultdict(list)
            for m in STREAM_PARSER_RE.finditer(q):
                d[m.group('name')].append(unquote(m.group('value')))
            yield dict(d)

        try:
            params['resumptionToken'] = RESUMPTION_TOKEN_RE.search(r.text).group('token')
        except Exception as e:
            raise StopIteration

        if 'resumptionToken' in params and 'metadataPrefix' in params:
            del params['metadataPrefix']


d = []
for r in fetch_calm_records():
    d.append(r)
    if len(d) % 1000 == 0:
        print(f'{len(d)}...')

json.dump(d, open('calm_records.json', 'w'), indent=2, sort_keys=True)
