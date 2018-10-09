# -*- encoding: utf-8

from flask_restplus import Api

from .bags import api as bags_api
from .ingests import api as ingests_api

api = Api(
    version="0.1",
    title="Archive API",
    description=(
        "A service to ingest and archive BagIt "
        "(https://tools.ietf.org/html/draft-kunze-bagit-17) resources"
    ),
    prefix="/storage/v1",
    doc=None
)

api.add_namespace(bags_api)
api.add_namespace(ingests_api)
