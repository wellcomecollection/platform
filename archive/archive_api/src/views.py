# -*- encoding: utf-8


from flask import jsonify, make_response, request
from flask_restplus import Resource
from werkzeug.exceptions import BadRequest as BadRequestError
from werkzeug.exceptions import NotFound as NotFoundError

from archive_api import app, api, logger
from ingests import send_new_ingest_request
import models
from progress_manager import ProgressNotFoundError
import validators


# progress_manager = app.config["PROGRESS_MANAGER"]

# api.add_model(name="Error", definition=models.Error)


@app.route("/storage/v1/healthcheck")
def route_report_healthcheck_status():
    return {"status": "OK"}
