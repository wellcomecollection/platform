# -*- encoding: utf-8

import daiquiri
from flask import abort, make_response, request, url_for
from flask_restplus import Namespace, Resource

from ingests import send_new_ingest_request
from models.catalogue import Error
from models.ingests import Ingest, IngestType, IngestStatus, Space, Callback, IngestResource
from models.progress import Progress, ProgressEvent
from progress_manager import ProgressNotFoundError
import validators


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
    @api.expect(Ingest, validate=True)
    @api.response(201, "Ingest created")
    @api.response(400, "Bad request", Error)
    def post(self):
        """Create a request to ingest a BagIt resource"""
        upload_url = request.json["uploadUrl"]
        callback_url = request.json.get("callbackUrl")
        space_type = request.json.get("space")
        space = space_type["id"]

        self._validate_urls(callback_url, upload_url)

        from archive_api import app

        progress_manager = app.config["PROGRESS_MANAGER"]

        ingest_request_id = progress_manager.create_request(
            upload_url=upload_url, callback_uri=callback_url, space=space
        )
        logger.debug("ingest_request_id=%r", ingest_request_id)

        ingest_request_id = send_new_ingest_request(
            sns_client=app.config["SNS_CLIENT"],
            topic_arn=app.config["SNS_TOPIC_ARN"],
            ingest_request_id=ingest_request_id,
            upload_url=upload_url,
            callback_url=callback_url,
        )

        # Construct the URL where the user will be able to get the status
        # of their ingest request.
        location = url_for(IngestResource.endpoint, id=ingest_request_id)

        # Now we set the Location response header.  There's no way to do this
        # without constructing our own Response object, so that's what we do
        # here.  See https://stackoverflow.com/q/25860304/1558022
        resp = make_response("", 201)
        resp.headers["Location"] = location
        return resp

    def _validate_urls(self, callback_url, upload_url):
        try:
            validators.validate_upload_url(upload_url)
        except ValueError as error:
            abort(400, f"Invalid uploadUrl:{upload_url!r}, {error}")

        if callback_url is not None:
            try:
                validators.validate_callback_url(callback_url)
            except ValueError as error:
                abort(400, f"Invalid callbackUrl:{callback_url!r}, {error}")


@api.route("/<id>")
@api.param("id", "The ingest request identifier")
class IngestResource(Resource):
    @api.doc(
        description="The ingest request id is returned in the Location header from a POSTed ingest request"
    )
    @api.marshal_with(Ingest)
    @api.response(200, "Ingest found")
    @api.response(404, "Ingest not found", Error)
    def get(self, id):
        """Get the current status of an ingest request"""
        try:
            from archive_api import app

            progress_manager = app.config["PROGRESS_MANAGER"]

            return progress_manager.lookup_progress(id=id)
        except ProgressNotFoundError as error:
            abort(404, f"Invalid id: No ingest found for id={id!r}")
