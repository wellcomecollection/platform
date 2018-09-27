# -*- encoding: utf-8

import os
import uuid

import daiquiri
from flask import Flask
from flask_restplus import Api, Resource
from flask import jsonify, make_response, request
from werkzeug.exceptions import BadRequest as BadRequestError

from report_ingest_status import report_ingest_status
from request_new_ingest import send_new_ingest_request

from create_ingest_progress import (
    IngestProgress,
    create_ingest_progress
)

from bag_request import bag_request

import config
import validators

app = Flask(__name__)
api = Api(app,
          version='0.1',
          title='Archive API',
          description='A service to ingest and archive BagIt '
                      '(https://tools.ietf.org/html/draft-kunze-bagit-17) resources',
          prefix='/storage/v1')

config_obj = config.ArchiveAPIConfig(
    development=(os.environ.get('FLASK_ENV') == 'development')
)

app.config.from_object(config_obj)

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()

api.namespaces.clear()
ns_ingests = api.namespace('ingests', description='Ingest requests')
ns_bags = api.namespace('bags', description='Bag requests')

# We can't move this import to the top because the models need the ``api``
# instance defined in this file.
import models  # noqa


@ns_ingests.route('')
@ns_ingests.doc(description='Request the ingest of a BagIt resource.')
@ns_ingests.param(
    'payload',
    'The ingest request specifying the uploadUrl where the BagIt resource can be found', _in='body')
class IngestCollection(Resource):
    @ns_ingests.expect(models.IngestRequest, validate=True)
    @ns_ingests.response(202, 'Ingest created')
    @ns_ingests.response(400, 'Bad request', models.Error)
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
            validators.validate_single_url(
                upload_url,
                supported_schemes=['s3'],
                allow_fragment=False
            )
        except ValueError as error:
            raise BadRequestError(f"Invalid uploadUrl:{upload_url!r}, {error}")
        if callback_url:
            try:
                validators.validate_single_url(
                    callback_url,
                    supported_schemes=['http', 'https']
                )
            except ValueError as error:
                raise BadRequestError(f"Invalid callbackUrl:{callback_url!r}, {error}")


@ns_ingests.route('/<string:id>')
@ns_ingests.param('id', 'The ingest request identifier')
class IngestResource(Resource):
    @ns_ingests.doc(description='The ingest request id is returned in the Location header from a POSTed ingest request')
    @ns_ingests.response(200, 'Ingest found')
    @ns_ingests.response(404, 'Ingest not found', models.Error)
    def get(self, id):
        """Get the current status of an ingest request"""
        result = report_ingest_status(
            dynamodb_resource=app.config['DYNAMODB_RESOURCE'],
            table_name=app.config['DYNAMODB_TABLE_NAME'],
            guid=id
        )
        return jsonify(result)


@ns_bags.route('/<string:id>')
@ns_bags.param('id', 'The bag identifier')
class BagResource(Resource):
    @ns_bags.doc(description='The bag is returned in the body of the response')
    @ns_bags.response(200, 'Bag found')
    @ns_bags.response(404, 'Bag not found', models.Error)
    def get(self, id):
        """Get the bag associated with an id"""
        result = bag_request(
            dynamodb_resource=app.config['DYNAMODB_RESOURCE'],
            table_name=app.config['BAG_VHS_TABLE_NAME'],
            s3_client=app.config['S3_CLIENT'],
            bucket_name=app.config['BAG_VHS_BUCKET_NAME'],
            id=id
        )
        return jsonify(result)


@app.route('/storage/v1/healthcheck')
def route_report_healthcheck_status():
    return jsonify({'status': 'OK'})


@app.errorhandler(Exception)
@api.errorhandler(Exception)
# @api.marshal_with(models.Error)
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
