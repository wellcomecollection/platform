# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask

from apis import api
import config
from models.catalogue import Error
from responses import ContextResponse
from progress_manager import ProgressManager

app = Flask(__name__)
app.response_class = ContextResponse

api.init_app(app)

config_obj = config.ArchiveAPIConfig(
    development=(os.environ.get("FLASK_ENV") == "development")
)

app.config.from_object(config_obj)

daiquiri.setup(level=os.environ.get("LOG_LEVEL", "INFO"))
logger = daiquiri.getLogger()

app.config["PROGRESS_MANAGER"] = ProgressManager(
    endpoint=app.config["PROGRESS_MANAGER_ENDPOINT"],
    sess=app.config["PROGRESS_MANAGER_SESSION"],
)


@app.errorhandler(Exception)
@api.errorhandler(Exception)
@api.marshal_with(Error, skip_none=True)
def default_error_handler(error):
    error_response = {
        "httpStatus": getattr(error, "code", 500),
        "label": getattr(error, "name", "Internal Server Error"),
    }
    logger.warn(error)
    if error_response["httpStatus"] != 500:
        if hasattr(error, "data"):
            error_response["description"] = ", ".join(
                error.data.get("errors", {}).values()
            )

            # The ``data`` attribute is set on ``ValidationError`` if
            # the user sends a model that doesn't validate.  If we pass this
            # attribute along to the Flask-RESTPlus error handlers, they
            # discard our custom response and use the "data" value instead.
            #
            # So we delete the ``data`` attribute here, and then Flask-RESTPlus
            # leaves our custom error model alone.
            #
            # For more details, see
            # https://github.com/noirbizarre/flask-restplus/issues/530
            #
            del error.data

        else:
            error_response["description"] = getattr(error, "description", str(error))
    return error_response, error_response["httpStatus"]


@app.route("/storage/v1/healthcheck")
def route_report_healthcheck_status():
    return {"status": "OK"}


if __name__ == "__main__":
    app.run(debug=True)
