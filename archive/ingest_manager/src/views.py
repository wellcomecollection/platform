# -*- encoding: utf-8

from flask import jsonify

from ingest_manager import app
from report_ingest_status import report_ingest_status


@app.route('/ingests/<guid>')
def route_report_ingest_status(guid):
    result = report_ingest_status(
        dynamodb_resource=app.config['dynamodb_resource'],
        table_name=app.config['dynamodb_table_name'],
        guid=guid
    )
    return jsonify(result)
