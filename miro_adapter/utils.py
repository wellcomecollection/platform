# -*- encoding: utf-8 -*-


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
    bad_values = (
        b'\x13', b'\x14', b'\x18', b'\x19', b'\x1b', b'\x7f', b'\xa3', b'\xae',
        b'\xe1', b'\xe9'
    )
    for v in bad_values:
        v = xml_string.replace(v, b'_')
    return xml_string
