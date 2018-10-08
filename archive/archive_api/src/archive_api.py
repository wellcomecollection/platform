# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask
from flask_restplus import Api

from apis import bags_api
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

# We can't move this import to the top because the views need the ``api``
# instance defined in this file.
from views import *  # noqa


if __name__ == "__main__":
    app.run(debug=True)
