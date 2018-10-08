# -*- encoding: utf-8

from archive_api import app


@app.route("/storage/v1/healthcheck")
def route_report_healthcheck_status():
    return {"status": "OK"}
