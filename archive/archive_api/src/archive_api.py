# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask
from flask_restplus import Api

from apis import bags_api, ingests_api
import config
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

import models

from werkzeug.exceptions import HTTPException

api.add_model(name="Error", definition=models.Error)

@app.errorhandler(Exception)
@api.errorhandler(Exception)
@bag_api.errorhandler(Exception)
@ingests_api.errorhandler(Exception)
@ingests_api.errorhandler(HTTPException)
@api.marshal_with(models.Error, skip_none=True)
def default_error_handler2(error):
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

            # This is the key line.  Look in api.py (screenshot on Desktop) for context
            del error.data
        else:
            error_response["description"] = getattr(error, "description", str(error))

    # print(type(error))
    # print(dir(error))
    # print(error.args)
    # print(error.code)
    # print(error.data)

    print(error_response)
    print('@@AWLC description=%r' % error_response.get("description"))
    print('@@AWLC error_response=%r' % error_response)
    return error_response, error_response["httpStatus"]

print(api.error_handlers)


api.add_namespace(bag_api)
api.add_namespace(ingests_api)


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

# We can't move this import to the top because the views need the ``api``
# instance defined in this file.
from views import *  # noqa

import models


if __name__ == "__main__":
    app.run(debug=True)
