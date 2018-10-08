# -*- encoding: utf-8

from archive_api import app, api, logger


progress_manager = app.config["PROGRESS_MANAGER"]

api.namespaces.clear()


@app.route("/storage/v1/healthcheck")
def route_report_healthcheck_status():
    return {"status": "OK"}


# TODO: There's no testing of the error handling; we should fix that!
@app.errorhandler(Exception)
@api.errorhandler(Exception)
def default_error_handler(error):
    error_response = {
        "errorType": "http",
        "httpStatus": getattr(error, "code", 500),
        "label": getattr(error, "name", "Internal Server Error"),
        "type": "Error",
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
