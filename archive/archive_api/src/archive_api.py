# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask
from flask import jsonify, make_response, request, url_for
from werkzeug.exceptions import BadRequest as BadRequestError

from report_ingest_status import report_ingest_status
from request_new_ingest import send_new_ingest_request

app = Flask(__name__)
app.config.from_object('config')

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


@app.route('/healthcheck')
def route_report_healthcheck_status():
    return jsonify({'status': 'OK'})


@app.route('/ingests/<guid>')
def route_report_ingest_status(guid):
    result = report_ingest_status(
        dynamodb_resource=app.config['dynamodb_resource'],
        table_name=app.config['dynamodb_table_name'],
        guid=guid
    )
    return jsonify(result)


@app.route('/ingests', methods=['POST'])
def route_request_new_ingest():
    try:
        upload_url = request.form['uploadUrl']
    except KeyError:
        raise BadRequestError('No uploadUrl parameter in request')

    callback_url = request.form.get('callbackUrl')

    ingest_request_id = send_new_ingest_request(
        sns_client=app.config['sns_client'],
        topic_arn=app.config['sns_topic_arn'],
        upload_url=upload_url,
        callback_url=callback_url
    )

    # Construct the URL where the user will be able to get the status
    # of their ingest request.
    location = url_for(
        'route_report_ingest_status',
        guid=ingest_request_id
    )

    # Now we set the Location response header.  There's no way to do this
    # without constructing our own Response object, so that's what we do
    # here.  See https://stackoverflow.com/q/25860304/1558022
    resp = make_response(location, 202)
    resp.headers['Location'] = location

    return resp
