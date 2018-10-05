# -*- encoding: utf-8

import models


def register_error_handler(namespace):
    """
    Register an error handler on the namespace.  This causes exceptions
    to be returned in the same model as the Catalogue API.

    See https://flask-restplus.readthedocs.io/en/stable/errors.html

    """
    @namespace.errorhandler(Exception)
    @namespace.marshal_with(models.Error, skip_none=True)
    def default_error_handler(error):
        error_response = {
            "httpStatus": getattr(error, "code", 500),
            "label": getattr(error, "name", "Internal Server Error"),
        }
        logger.warn(error)

        if error_response["httpStatus"] == 500:
            description = None
        else:
            try:
                description = ", ".join(error.data.get("errors", {}).values())
            except AttributeError:
                description = getattr(error, "description", str(error))
        error_response["description"] = description

        return error_response, error_response["httpStatus"]
