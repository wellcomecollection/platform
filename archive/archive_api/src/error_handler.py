# -*- encoding: utf-8

import models


def register_error_handler(namespace):
    """
    Register an error handler on the namespace.  This causes exceptions
    to be returned in the same model as the Catalogue API.

    See https://flask-restplus.readthedocs.io/en/stable/errors.html

    """
    @namespace.errorhandler(Exception)
    @namespace.marshal_with(models.Error)
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