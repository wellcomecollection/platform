#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import json
import sys

from requests_futures.sessions import FuturesSession

from calm_to_dynamodb import find_new_records
from mapping_collection import MAPPING


keys = MAPPING['properties'].keys()


session = FuturesSession()


for i, r in enumerate(find_new_records(sys.argv[1])):
    if i % 100 == 0:
        print('.', end='')
    data = {
        'AltRefNo': r['AltRefNo']
    }
    record_data = json.loads(r['data'])
    for k in keys:
        if k == 'AltRefNo':
            continue
        value = record_data.get(k)
        data[k] = value
    session.post(
        'http://localhost:9200/calm_index/%s' % r['RecordType'].lower(),
        data=json.dumps(data))