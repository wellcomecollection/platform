#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""Checks for the validity of Calm data.

This script runs a series of checks over the Calm data, looking for problems
that should be corrected in the source data.

"""

import json

from utils import read_records


WARNINGS = {}


def check_record(records):
    try:
        for record in records:
            _check_exactly_one_field(record, 'AltRefNo')
            _check_exactly_one_field(record, 'RefNo')
    except KeyboardInterrupt:
        pass
    json.dump(WARNINGS, open('calm_warnings.json', 'w'), indent=2)
    print('\nWritten warnings to calm_warnings.json')


def _warn(record, message, context_data=None):
    """Drop a message about an inconsistent record.

    :param record: A record returned by read_records()
    :param message: A string explaining the problem
    :param context_data: Any record-specific contextual data

    """
    record_id = record.find('RecordID').text
    if message not in WARNINGS:
        if context_data is None:
            WARNINGS[message] = []
        else:
            WARNINGS[message] = {}

    if context_data is None:
        WARNINGS[message].append(record_id)
    else:
        WARNINGS[message][record_id] = context_data


def _check_exactly_one_field(record, field):
    """Checks that a record has exactly one, non-empty instance of
    a given field.

    :param record: A record returned by read_records()
    :param field: Name of the Calm field to inspect

    """
    fields = record.getchildren()
    field_names = [c.tag for c in fields]
    if fields.count(field) == 0:
        _warn(record, 'Missing %r field' % field)
    elif fields.count(field) != 1:
        _warn(record, 'Too many instances of %r field' % field,
              [f.text for f in fields if f.tag == field])
    elif fields.find(field).text is None:
        _warn(record, 'Empty %r field' % field)


if __name__ == '__main__':
    import sys
    check_record(read_records(sys.argv[1]))
