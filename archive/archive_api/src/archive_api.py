# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask
from flask_restplus import Api

from apis import bags_api, ingests_api
import config
import models
from responses import ContextResponse
from progress_manager import ProgressManager

app = Flask(__name__)
app.response_class = ContextResponse

api = Api(
    app,
    version="0.1",
    title="Archive API",
    description="A service to ingest and archive BagIt "
    "(https://tools.ietf.org/html/draft-kunze-bagit-17) resources",
    prefix="/storage/v1",
)

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

api.add_namespace(bags_api)
api.add_namespace(ingests_api)


# TODO: There's no testing of the error handling; we should fix that!
@app.errorhandler(Exception)
@api.errorhandler(Exception)
@bags_api.errorhandler(Exception)
@ingests_api.errorhandler(Exception)
@api.marshal_with(models.Error, skip_none=True)
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
        else:
            error_response["description"] = getattr(error, "description", str(error))
    return error_response, error_response["httpStatus"]


# We can't move this import to the top because the views need the ``api``
# instance defined in this file.
from views import *  # noqa


if __name__ == "__main__":
    app.run(debug=True)
