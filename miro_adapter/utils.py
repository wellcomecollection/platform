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
    bad_values = {
        b'\x14', b'\x1b', b'\x7f', b'\x81', b'\x8d', b'\x9d', b'\xa0', b'\xa1',
        b'\xa2', b'\xa3', b'\xa4', b'\xa5', b'\xa6', b'\xa7', b'\xa8', b'\xa9',
        b'\xaa', b'\xab', b'\xac', b'\xad', b'\xae', b'\xaf', b'\xb0', b'\xb1',
        b'\xb2', b'\xb3', b'\xb4', b'\xb5', b'\xb6', b'\xb8', b'\xb9', b'\xba',
        b'\xbb', b'\xbc', b'\xbd', b'\xbe', b'\xbf', b'\xc0', b'\xc1', b'\xc2',
        b'\xc3', b'\xc4', b'\xc5', b'\xc6', b'\xc8', b'\xc9', b'\xca', b'\xcc',
        b'\xce', b'\xd3', b'\xd4', b'\xd6', b'\xdc', b'\xde', b'\xe0', b'\xe1',
        b'\xe2', b'\xe3', b'\xe4', b'\xe5', b'\xe6', b'\xe7', b'\xe8', b'\xe9',
        b'\xea', b'\xeb', b'\xec', b'\xed', b'\xee', b'\xef', b'\xf1', b'\xf2',
        b'\xf3', b'\xf4', b'\xf5', b'\xf6', b'\xf9', b'\xfa', b'\xfb', b'\xfc',
        b'\xff'
    }
    for v in bad_values:
        v = xml_string.replace(v, b'_')
    return xml_string
