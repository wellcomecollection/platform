#!/usr/bin/env python3
# -*- encoding: utf-8 -*-
"""
Given a JSON dump of the OAI-PMH harvest, run through the AltRefNo field
and look for discrepancies.  In particular:

* Records that don't have an AltRefNo or more than one AltRefNo.
* AltRefNo's that are associated with more than one record.

"""

import collections
import json

data = json.load(open('calm_records.json'))

altrefno_to_record_id = collections.defaultdict(list)
bad_records = []

for d in data:
    alt_ref_no = d['AltRefNo']
    record_id = d['RecordID'][0]

    # Is there anything other than one AltRefNo field?
    if len(alt_ref_no) != 1:
        bad_records.append(record_id)

    for a in alt_ref_no:
        altrefno_to_record_id[a].append(record_id)

print('Records with =/= 1 AltRefNo values:')
for r_id in bad_records:
    print(r_id)

print()
print('AltRefNos attached to more than one record:')
for m, v in altrefno_to_record_id.items():
    if len(v) > 1:
        print('%s\t%s' % (m, v))
