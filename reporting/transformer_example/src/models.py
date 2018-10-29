from attr import attrs, attrib

@attrs
class ElasticsearchConfig(object):
    url = attrib()
    username = attrib()
    password = attrib()
    index = attrib()
    doc_type = attrib()


@attrs
class ObjectLocation(object):
    namespace = attrib()
    key = attrib()


@attrs
class HybridRecord(object):
    id = attrib()
    location = attrib()


@attrs
class ElasticsearchRecord(object):
    id = attrib()
    doc = attrib()