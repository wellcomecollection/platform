#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This script ingests records from a Calm XML export, and pushes them into
DynamoDB with our given schema.
"""

from datetime import datetime
import json
import re
import tempfile

from lxml import etree


def find_new_records(path, last_ingest=None):
    """
    Generate new Calm records to be ingested into DynamoDB.

    :param path: Path to the Calm XML export file.
    :param last_ingest: Date of the last Calm -> DynamoDB ingest.
    """
    path = _correct_entities(path)

    # The Calm data set is ~330k records, so this function is built as a
    # sequence of generators that are iterated over to produce the records:
    # this saves us from loading the entire Calm export into memory at once.
    #
    # This work is still moderately expensive, and we should try to improve
    # performance wherever possible.
    #
    # This is based on the header validation logic in hyper-h2:
    # https://github.com/python-hyper/hyper-h2/blob/master/h2/utilities.py
    records = _retrieve_elements(path)
    records = _skip_updated_records(records, last_ingest)
    records = _prepare_record_for_dynamodb(records)

    yield from records


# TODO: This step incurs a significant performance penalty, because we do
# a lot of I/O to read and rewrite the document.  It should be possible to
# stream these bytes directly into etree.iterparse, thereby saving a step.
def _correct_entities(path):
    """
    Correct the XML entities in the Calm XML export.

    The XML standards only defined a few entities, and the Calm exporter
    includes some entities that cause lxml to throw an XMLSyntaxError.
    This function creates a new file that has the syntax-corrected entities,
    and returns a path to the new file.

    :param path: Path to the Calm XML export file.
    :returns: Path to the corrected Calm XML export file.
    """
    _, tmppath = tempfile.mkstemp(suffix='.xml', prefix='calmexport_')

    # Regex that matches ampersand-quoted entities that aren't predefined.
    # An HTML entity is something like '&rsquo;', which needs correcting to
    # '&amp;rsquo;' to be valid XML.
    #
    # A list of predefined entities can be found on Wikipedia:
    # https://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
    entity_regex = re.compile(
        r'&'                         # opening ampersand
        r'(?!quot|amp|apos|lt|gt)'   # negative lookahead group that ignores
                                     # predefined entities
        r'(?P<entity>[A-Za-z0-9]+)'  # entity body
        r';'                         # closing semicolon
    )

    # At the same time, patch up any ampersands that aren't escaped
    # correctly.  A couple have slipped through that break the parser.
    amp_regex = re.compile(r'&(?!amp;)')

    with open(path) as infile, open(tmppath, 'w') as outfile:
        # Loading the entire file into memory and then doing the
        # find-and-replace would be very slow.  We proceed line-by-line, as
        # we've yet to see illegal entities split across multiple lines.
        for line in infile:
            line = entity_regex.sub('&amp;\g<entity>;', line)
            line = amp_regex.sub('&amp;', line)
            outfile.write(line)
    return tmppath


def _retrieve_elements(path):
    """
    Generate a sequence of Calm records from an XML document.

    :param path: Path to the Calm XML export file.
    """
    # The format of the Calm XML export file is
    #
    #     <?xml version="1.0" encoding="ISO-8859-1" ?>
    #     <!DOCTYPE DScribeDatabase SYSTEM "CALM_XML_all_data_20170124.dtd">
    #     <DScribeDatabase Name="Catalog">
    #       <DScribeRecord>
    #         ...
    #       </DScribeRecord>
    #       <DScribeRecord>
    #         ...
    #       </DScribeRecord>
    #     </DScribeDatabase>
    #
    # Each record is contained in a single <DScribeRecord> tag, so we're
    # just interested in extracting those.
    context = etree.iterparse(path, events=('end',), tag='DScribeRecord', resolve_entities=False)
    for entry in context:
        yield entry[1]


def _skip_updated_records(records, since_date=None):
    """
    Generate a sequence of Calm records that were updated on or after
    a given date.
    """
    # If we don't have a date, return everything
    if since_date is None:
        yield from records

    else:
        for record in records:
            # The modification/creation date of a record is kept in the
            # <Modified> and <Created> tags, respectively.
            #
            # Some records do not have these tags, or their value is empty:
            # then we always include the record, just in case.
            modified = record.find('Modified')
            if (modified is None) or (modified.text is None):
                modified_str = record.find('Created').text
            else:
                modified_str = modified.text

            # The format of these fields is DD/MM/YYYY.
            if modified_str is None:
                yield record
            else:
                modified = datetime.strptime(modified_str, '%d/%m/%Y').date()
                if modified >= since_date:
                    yield record


def _prepare_record_for_dynamodb(records):
    """
    Generate a sequence of dictionaries that can be inserted into DynamoDB.
    """
    for record in records:
        # Produce a dict of all the attributes on the record that aren't
        # stored in one of the other Dynamo fields.  We'll convert this to
        # a JSON string and store the encoded output, so discarding fields
        # we already have improves performance and storage.
        data = {
            c.tag: c.text
            for c in record.getchildren()
            if c.tag not in ('RecordID', 'RecordType', 'RefNo', 'AltRefNo')
        }

        yield {
            'RecordID': record.find('RecordID').text,
            'RecordType': record.find('RecordType').text,
            'RefNo': record.find('RefNo').text,
            'AltRefNo': record.find('AltRefNo').text,
            'data': json.dumps(data),
        }


if __name__ == '__main__':
    # To help us design the schema, this snippet will print a list of all
    # the fields on each record (along with their frequency).
    from collections import Counter
    from pprint import pprint
    import sys

    import boto3

    dynamodb = boto3.resource('dynamodb')
    table = dynamodb.Table('CalmData')

    # We do some bookkeeping to avoid sending the same record to DynamoDB
    # multiple times, which is both slow and unnecessary cost.  The full
    # Calm export has ~331k records, so the more we can skip, the better.
    try:
        existing = json.load(open('existing.json', 'r'))
    except OSError:
        existing = []

    for i, record in enumerate(find_new_records(sys.argv[1])):

        # Have we already sent this record to DynamoDB?
        if record['RecordID'] in existing:
            continue

        r = table.put_item(Item=record)
        assert r['ResponseMetadata']['HTTPStatusCode'] == 200

        # Drop a record immediately that we've added this to DynamoDB.
        existing.append(record['RecordID'])
        json.dump(existing, open('existing.json', 'w'))
