# -*- encoding: utf-8

import os
from urllib.parse import urlparse
import uuid

import daiquiri
from flask import Flask
from flask import jsonify, make_response, request, url_for
from werkzeug.exceptions import BadRequest as BadRequestError

from report_ingest_status import report_ingest_status
from request_new_ingest import send_new_ingest_request

from create_ingest_progress import (
    IngestProgress,
    create_ingest_progress
)

app = Flask(__name__)
app.config.from_object('config.ProductionConfig')

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()


@app.route('/healthcheck')
def route_report_healthcheck_status():
    return jsonify({'status': 'OK'})


@app.route('/ingests/<guid>')
def route_report_ingest_status(guid):
    result = report_ingest_status(
        dynamodb_resource=app.config['DYNAMODB_RESOURCE'],
        table_name=app.config['DYNAMODB_TABLE_NAME'],
        guid=guid
    )
    return jsonify(result)


@app.route('/ingests', methods=['POST'])
def route_request_new_ingest():
    if not request.is_json:
        raise BadRequestError('Mimetype expected to be application/json')

    try:
        request_data = request.get_json()
        validate_type(request_data['type'])
        validate_ingest_type(request_data['ingestType'])
        upload_url = request_data['uploadUrl']
        callback_url = request_data.get('callbackUrl')
    except BadRequestError:
        raise BadRequestError('Invalid json in request')
    except KeyError as key_error:
        raise BadRequestError(f'No {key_error.args[0]} parameter in request')
    except ValueError as value_error:
        raise BadRequestError(f'{value_error} in request')

    # TODO: There is quite a lot of request validation logic which should be moved out
    try:
        validate_url(upload_url, supported_schemes=['s3'], allow_fragment=False)
    except ValueError as error:
        raise BadRequestError(f"Invalid uploadUrl:{upload_url!r}, {error}")

    if callback_url:
        try:
            validate_url(callback_url, supported_schemes=['http', 'https'])
        except ValueError as error:
            raise BadRequestError(f"Invalid callbackUrl:{callback_url!r}, {error}")

    ingest_request_id = str(uuid.uuid4())
    logger.debug('ingest_request_id=%r', ingest_request_id)

    create_ingest_progress(
        IngestProgress(ingest_request_id, upload_url, callback_url),
        app.config['DYNAMODB_RESOURCE'],
        app.config['DYNAMODB_TABLE_NAME'])

    ingest_request_id = send_new_ingest_request(
        sns_client=app.config['SNS_CLIENT'],
        topic_arn=app.config['SNS_TOPIC_ARN'],
        ingest_request_id=ingest_request_id,
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
    resp = make_response('', 202)
    resp.headers['Location'] = location
    return resp


@app.errorhandler(BadRequestError)
def bad_request_error(error):
    return jsonify(status=400, message=error.description), 400


def validate_type(type_value):
    if type_value != 'Ingest':
        raise ValueError(f'Expected \'type\'=\'Ingest\', got {type_value!r}')


def validate_ingest_type(ingest_type_value):
    valid_ingest_type = {'id': 'create', 'type': 'IngestType'}
    if ingest_type_value != valid_ingest_type:
        raise ValueError(f'Expected \'ingestType\'={valid_ingest_type!r}, got {ingest_type_value!r}')


def validate_url(url, supported_schemes=None, allow_fragment=True):
    """
    Validates the passed string is a URL, optionally checking against allowed schemes and
    whether a fragment is allowed
    """
    parsed_url = urlparse(url)
    incomplete_url = any(not p for p in [parsed_url.scheme, parsed_url.netloc])
    invalid_scheme = (supported_schemes and parsed_url.scheme not in supported_schemes)
    unallowed_fragment = (not allow_fragment and bool(parsed_url.fragment))
    if incomplete_url or invalid_scheme or unallowed_fragment:
        errors = []
        if incomplete_url:
            errors.append(f'is not a complete url')
        if invalid_scheme:
            errors.append(f'{parsed_url.scheme!r} is not one of the supported schemes ({supported_schemes!r})')
        if unallowed_fragment:
            errors.append(f'{parsed_url.fragment!r} fragment is not supported')
        raise ValueError(','.join(errors))
