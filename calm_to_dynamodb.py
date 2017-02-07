#!/usr/bin/env python
# -*- encoding: utf-8 -*-
"""
This script ingests records from a Calm XML export, and pushes them into
DynamoDB with our given schema.
"""

from datetime import datetime
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


if __name__ == '__main__':
    # To help us design the schema, this snippet will print a list of all
    # the fields on each record (along with their frequency).
    from collections import Counter
    from pprint import pprint
    import sys

    i = 0
    fields = Counter()
    for record in find_new_records(sys.argv[1]):
        i += 1
        fields.update([c.tag for c in record.getchildren()])
    pprint(fields)
    print('%d records' % i)
