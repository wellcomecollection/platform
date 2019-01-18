import xml.etree.ElementTree as ElTree

# premis is the addition
namespaces = {
    "mets": "http://www.loc.gov/METS/",
    "mods": "http://www.loc.gov/mods/v3",
    "tessella": "http://www.tessella.com/transfer",
    "dv": "http://dfg-viewer.de/",
    "xlink": "http://www.w3.org/1999/xlink",
    "premis": "http://www.loc.gov/premis/v3",
}

for prefix, ns in namespaces.items():
    ElTree.register_namespace(prefix, ns)


def copy_simple_value_child(source_parent, source_name, target_parent, target_name):
    source = source_parent.find(source_name, namespaces)
    target = make_child_with_whitespace(target_parent, target_name)
    target.text = source.text


def make_child_with_whitespace(parent, name):
    # This isn't quite right but tidier than nothing
    child = ElTree.SubElement(parent, name)
    child.text = parent.text + "  "
    child.tail = parent.text
    return child


def remove_first_child(element):
    element.remove(element[0])


def expand(namespace, attr_name):
    return "{{{0}}}{1}".format(namespaces[namespace], attr_name)


def make_child(parent, namespace, attr_name):
    expanded_form = expand(namespace, attr_name)
    return make_child_with_whitespace(parent, expanded_form)


def load_from_disk(path):
    return ElTree.parse(path)


def load_from_string(xml_string):
    root = ElTree.fromstring(xml_string)
    return ElTree.ElementTree(root)
