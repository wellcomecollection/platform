# -*- encoding: utf-8

import os
from urllib.parse import urlparse
import uuid

import daiquiri
from flask import Flask
from flask_restplus import Api, Resource, fields
from flask import jsonify, make_response, request
from werkzeug.exceptions import BadRequest as BadRequestError

from report_ingest_status import report_ingest_status
from request_new_ingest import send_new_ingest_request

from create_ingest_progress import (
    IngestProgress,
    create_ingest_progress
)

app = Flask(__name__)
api = Api(app,
          version='0.1',
          title='Archive API',
          description='A service to ingest and archive BagIt '
                      '(https://tools.ietf.org/html/draft-kunze-bagit-17) resources',
          prefix='/storage/v1')

if os.environ.get('FLASK_ENV') == 'development':
    app.config.from_object('config.DevelopmentConfig')
else:
    app.config.from_object('config.ProductionConfig')

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()

api.namespaces.clear()
ns = api.namespace('ingests', description='Ingest requests')

ingest_request_model = api.model('Ingest request', {
    'type': fields.String(description='Type of the object', enum=['Ingest'], required=True),
    'uploadUrl': fields.String(description='URL of uploaded BagIt resource, supports only a zipped BagIt file', required=True),
    'callbackUrl': fields.String(description='URL to use for callback on completion or failure'),
    'ingestType': fields.Nested(api.model('Ingest type', {
        'type': fields.String(description='Type of the object', enum=['IngestType'], required=True),
        'id': fields.String(description='Identifier for ingest type', enum=['create'], required=True),
    }), description="Request to ingest a BagIt resource", required=True)
})

error_model = api.model('Error', {
    '@context': fields.String(description='Context URL'),
    'errorType': fields.String(description='errorType'),
    'httpStatus': fields.Integer(description='httpStatus'),
    'label': fields.String(description='label'),
    'description': fields.String(description='description of the error'),
    'type': fields.String(description='type'),
})


@ns.route('')
@ns.doc(description='Request the ingest of a BagIt resource.')
@ns.param('payload', 'The ingest request specifying the uploadUrl where the BagIt resource can be found', _in='body')
class IngestCollection(Resource):
    @ns.expect(ingest_request_model, validate=True)
    @ns.response(202, 'Ingest created')
    @ns.response(400, 'Bad request', error_model)
    def post(self):
        """Create a request to ingest a BagIt resource"""
        upload_url = request.json['uploadUrl']
        callback_url = request.json.get('callbackUrl')
        self.validate_urls(callback_url, upload_url)

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
        location = api.url_for(
            IngestResource,
            id=ingest_request_id
        )

        # Now we set the Location response header.  There's no way to do this
        # without constructing our own Response object, so that's what we do
        # here.  See https://stackoverflow.com/q/25860304/1558022
        resp = make_response('', 202)
        resp.headers['Location'] = location
        return resp

    def validate_urls(self, callback_url, upload_url):
        try:
            validate_url(upload_url, supported_schemes=['s3'], allow_fragment=False)
        except ValueError as error:
            raise BadRequestError(f"Invalid uploadUrl:{upload_url!r}, {error}")
        if callback_url:
            try:
                validate_url(callback_url, supported_schemes=['http', 'https'])
            except ValueError as error:
                raise BadRequestError(f"Invalid callbackUrl:{callback_url!r}, {error}")


@ns.route('/<string:id>')
@ns.param('id', 'The ingest request identifier')
class IngestResource(Resource):
    @ns.doc(description='The ingest request id is returned in the Location header from a POSTed ingest request')
    @ns.response(200, 'Ingest found')
    @ns.response(404, 'Ingest not found', error_model)
    def get(self, id):
        """Get the current status of an ingest request"""
        result = report_ingest_status(
            dynamodb_resource=app.config['DYNAMODB_RESOURCE'],
            table_name=app.config['DYNAMODB_TABLE_NAME'],
            guid=id
        )
        return jsonify(result)


@app.route('/healthcheck')
def route_report_healthcheck_status():
    return jsonify({'status': 'OK'})


@app.errorhandler(Exception)
@api.errorhandler(Exception)
# @api.marshal_with(error_model)
def default_error_handler(error):
    error_response = {
        '@context': 'https://api.wellcomecollection.org/storage/v1/context.json',
        'errorType': 'http',
        'httpStatus': getattr(error, 'code', 500),
        'label': getattr(error, 'name', 'Internal Server Error'),
        'type': 'Error',
    }
    if error_response['httpStatus'] != 500:
        if hasattr(error, 'data'):
            error_response['description'] = ', '.join(error.data.get('errors', {}).values())
        else:
            error_response['description'] = getattr(error, 'description', str(error))
    return jsonify(error_response), error_response['httpStatus']


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
