# -*- encoding: utf-8 -*-


def _render_value(value):
    """Renders a value."""
    if value is None or value == 'None':
        return None
    else:
        try:
            return value.strip()
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

        # Miro stores lists with <_> keys, so if we spot one, this element
        # should actually be treated as a list.
        if name == '_':
            return _elem_to_list(elem)

        if child.getchildren():
            res[name] = elem_to_dict(child)
        else:
            res[name] = _render_value(child.text)
    return res
