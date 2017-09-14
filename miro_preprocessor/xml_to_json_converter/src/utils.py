# -*- encoding: utf-8 -*-

import json

import boto3
from lxml import etree
from lazyreader import lazyread


def _render_value(value):
    """Renders a value."""
    if value is None or value == 'None':
        return None
    else:
        try:
            return value.strip() or None
        except AttributeError:
            return value


def _elem_to_list(elem):
    """Convert an lxml list-like Element to a Python list.

    Some of the Miro data stores lists in XML by using ``<_>`` keys, e.g.:

        <fruit>
            <_>apple</_>
            <_>banana</_>
            <_>cherry</_>
        </fruit>

    This function takes such elements and turns them into lists.
    """
    assert all(child.tag == '_' for child in elem.iterchildren())
    return [_render_value(child.text) for child in elem.iterchildren()]


def elem_to_dict(elem):
    """Converts an lxml Element to a Python dict."""
    res = {}
    for child in elem.iterchildren():
        name = child.tag
        assert name not in res

        # Miro stores lists with <_> keys, so if we spot one, this element
        # should actually be treated as a list.
        if name == '_':
            return _elem_to_list(elem)

        if child.getchildren():
            res[name] = elem_to_dict(child)
        else:
            res[name] = _render_value(child.text)
    return res


def fix_miro_xml_entities(xml_string):
    """
    The Miro XML contains some weird Unicode entities that cause lxml
    to break.  For now, we just throw them away.

    TODO: Process these properly (what do they contain in the original Miro?)
    """
    bad_values = {
        b'\x1b', b'\x7f', b'\x1f', b'\x14',
    }
    for v in bad_values:
        xml_string = xml_string.replace(v, b'_')
    return xml_string


def chunked_s3_reader(bucket, key, delimiter):
    """
    Read a file from S3, generating everything up to ``delimiter`` in each
    iteration.  This avoids loading the whole file into memory.
    """
    client = boto3.client('s3')
    obj = client.get_object(Bucket=bucket, Key=key)
    for chunk in lazyread(obj['Body'], delimiter=b'</image>'):
        yield chunk


def read_image_chunks_from_s3(bucket, key):
    """
    Loading an entire XML file at once would be prohibitively expensive,
    but we only need one <image> ... </image> block at a time.
    """
    for chunk in chunked_s3_reader(bucket=bucket, key=key, delimiter=b'</image>'):

        # The last chunk of data from the file will be some XML closing matter,
        # which is uninteresting.
        if b'<image>' not in chunk:
            continue

        # We want to return complete XML blocks, so we split looking for the
        # opening <image> tag, and include it on the result.
        chunk = chunk.split(b'<image>')[-1]
        yield b'<image>' + chunk


def read_json_lines_from_s3(bucket, key):
    """
    Read a document which contains one JSON document per line, and parse the
    JSON before passing to the caller.
    """
    for doc in chunked_s3_reader(bucket=bucket, key=key, delimiter=b'\n'):
        if doc:
            yield json.loads(doc)


def generate_images(bucket, key):
    # Because this is a stream parser, lxml doesn't know about the encoding
    # declaration at the top of the Miro XML exports.  We have to tell it.
    # It's a bit magic, but this is the easiest way to do it.
    iso_88591_parser = etree.XMLParser(encoding='iso-8859-1')
    for xml_chunk in read_image_chunks_from_s3(bucket, key):
        xml_string = fix_miro_xml_entities(xml_chunk)
        lxml_elem = etree.fromstring(xml_string, parser=iso_88591_parser)
        yield elem_to_dict(lxml_elem)
