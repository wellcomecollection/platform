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

        # We can't parse the Calm "XML" with an XML parser, because it isn't
        # actually valid XML.  Instead the values are URL-encoded as an
        # attribute on an XML-like tag, so we unpick those with a regex
        # and store the values that way.
        records = r.text.split('</record>')
        records.pop()
        for rec in records:
            d = collections.defaultdict(list)
            for m in STREAM_PARSER_RE.finditer(rec):
                d[m.group('name')].append(unquote(m.group('value')))
            yield dict(d)

        # Results from the OAI harvests are paginated, to prevent records
        # changing order under our feet.  The presence of a `resumptionToken`
        # tells us how to access the next page.
        try:
            params['resumptionToken'] = RESUMPTION_TOKEN_RE.search(r.text).group('token')
        except Exception:
            raise StopIteration

        if 'resumptionToken' in params and 'metadataPrefix' in params:
            del params['metadataPrefix']


all_records = []
for r in fetch_calm_records():
    all_records.append(r)
    if len(all_records) % 1000 == 0:
        print(f'{len(all_records)}...')

json.dump(
    all_records,
    open('calm_records.json', 'w'),
    indent=2, sort_keys=True
)
