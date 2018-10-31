# -*- encoding: utf-8

import daiquiri
from flask import abort, make_response, request
from flask_restplus import Namespace, Resource

from models.catalogue import Error
from models.ingests import (
    Ingest,
    IngestType,
    IngestStatus,
    Space,
    Callback,
    IngestResource,
)
from models.progress import Progress, ProgressEvent
from progress_manager import ProgressNotFoundError, ProgressServiceError


api = Namespace("ingests", description="Ingest requests")

api.add_model(name="Error", definition=Error)

api.add_model(name="Ingest", definition=Ingest)
api.add_model(name="IngestStatus", definition=IngestStatus)
api.add_model(name="IngestType", definition=IngestType)
api.add_model(name="Space", definition=Space)
api.add_model(name="Callback", definition=Callback)
api.add_model(name="IngestResource", definition=IngestResource)

api.add_model(name="Progress", definition=Progress)
api.add_model(name="ProgressEvent", definition=ProgressEvent)

logger = daiquiri.getLogger()


@api.route("")
@api.doc(description="Request the ingest of a BagIt resource.")
@api.param(
    "payload",
    "The ingest request specifying the uploadUrl where the BagIt resource can be found",
    _in="body",
)
class IngestCollection(Resource):
    # @api.expect(Ingest, validate=True)
    @api.response(201, "Ingest created")
    @api.response(400, "Bad request", Error)
    def post(self):
        """Create a request to ingest a BagIt resource"""

        from archive_api import app

        progress_manager = app.config["PROGRESS_MANAGER"]

        try:
            (location, json) = progress_manager.create_request(
                request_json=request.json
            )
        except ProgressServiceError as e:
            abort(e.status_code, e.message)
        resp = make_response(json, 201)
        resp.headers["Location"] = location
        return resp


@api.route("/<id>")
@api.param("id", "The ingest request identifier")
class IngestResource(Resource):
    @api.doc(
        description="The ingest request id is returned in the Location header from a POSTed ingest request"
    )
    # @api.marshal_with(Ingest)
    @api.response(200, "Ingest found")
    @api.response(404, "Ingest not found", Error)
    def get(self, id):
        """Get the current status of an ingest request"""
        try:
            from archive_api import app

            progress_manager = app.config["PROGRESS_MANAGER"]

            return progress_manager.lookup_progress(id=id)
        except ProgressNotFoundError:
            abort(404, f"Invalid id: No ingest found for id={id!r}")
