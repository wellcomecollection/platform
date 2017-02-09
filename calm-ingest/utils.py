# -*- encoding: utf-8 -*-
"""Utilities common across Calm ingest operations."""

import os
import re
import shutil
import tempfile

from lxml import etree


def read_records(path):
    """Read Calm records from the Calm XML export file.

    :param path: Path to the Calm XML export file.
    """
    path = _correct_entities(path)
    yield from _retrieve_elements(path)


# Regex that matches ampersand-quoted entities that aren't predefined.
# An HTML entity is something like '&rsquo;', which needs correcting to
# '&amp;rsquo;' to be valid XML.
#
# A list of predefined entities can be found on Wikipedia:
# https://en.wikipedia.org/wiki/List_of_XML_and_HTML_character_entity_references#Predefined_entities_in_XML
ENTITY_REGEX = re.compile(
    r'&'                         # opening ampersand
    r'(?!quot|amp|apos|lt|gt)'   # negative lookahead group that ignores
                                 # predefined entities
    r'(?P<entity>[A-Za-z0-9]+)'  # entity body
    r';'                         # closing semicolon
)


# Regex for matching ampersands that aren't XML-escaped correctly.
# A couple have slipped through that break our parser.
AMPERSAND_REGEX = re.compile(r'&(?!quot|amp|apos|lt|gt)')


def _correct_entities(path):
    """Correct the XML entities in the Calm XML export.

    The XML standards only defined a few entities, and the Calm exporter
    includes some entities that cause lxml to throw an XMLSyntaxError.
    This function creates a new file that has the syntax-corrected entities,
    and returns a path to the new file.

    :param path: Path to the Calm XML export file.
    :returns: Path to the corrected Calm XML export file.
    """
    # The name of the new file is the name of the old field with '_corrected'
    # appended.
    base_path, ext = os.path.splitext(path)
    new_path = ''.join([base_path, '_corrected', ext])

    # If this file already exists _and_ it was modified more recently than
    # the export file, we skip rebuilding it and assume it's correct.
    if os.path.exists(new_path):
        if os.path.getmtime(new_path) > os.path.getmtime(path):
            return new_path
        else:
            os.unlink(new_path)

    # Assume it doesn't exist.  Use a temporary file initially so the new path
    # created atomically.
    _, tmp_path = tempfile.mkstemp(suffix='.xml', prefix='calmexport_')

    # Write a corrected version of the XML to the tempfile.
    with open(path) as infile, open(tmp_path, 'w') as outfile:
        # Loading the entire file into memory and then doing the
        # find-and-replace would be very slow.  We proceed line-by-line, as
        # we've yet to see illegal entities split across multiple lines.
        for line in infile:
            line = ENTITY_REGEX.sub('&amp;\g<entity>;', line)
            line = AMPERSAND_REGEX.sub('&amp;', line)
            outfile.write(line)

    shutil.move(tmp_path, new_path)
    return new_path


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
    context = etree.iterparse(path, events=('end',), tag='DScribeRecord')

    # The iterator provides a sequence of (event, element) tuples.
    # The event is always the end of a 'DScribeRecord' tag, so ignore that.
    for entry in context:
        yield entry[1]
