# -*- encoding: utf-8

from flask_restplus import fields

from archive_api import api


def fieldType(name, **kwargs):
    return fields.String(
        description='Type of the object',
        enum=[name],
        **kwargs
    )


# Example of a valid request from the RFC:
#
#     {
#       "type": "Ingest",
#       "ingestType": {
#         "id": "create",
#         "type": "IngestType"
#       },
#       "uploadUrl": "s3://source-bucket/source-path/source-bag.zip",
#       "callbackUrl": "https://example.org/callback?id=b1234567",
#     }
#
ingest_type = api.model('Ingest type', {
    'type': fieldType(name='IngestType', required=True),
    'id': fields.String(
        description='Identifier for ingest type',
        enum=['create'],
        required=True
    ),
})

ingest_request_model = api.model('Ingest request', {
    'type': fieldType(name='Ingest', required=True),
    'uploadUrl': fields.String(
        description='S3 URL of uploaded BagIt resource, supports only a zipped BagIt file',
        example='s3://source-bucket/source-path/source-bag.zip',
        required=True,
    ),
    'callbackUrl': fields.String(
        description='URL to use for callback on completion or failure',
        example='https://workflow.wellcomecollection.org/callback?id=b1234567'
    ),
    'ingestType': fields.Nested(
        ingest_type,
        description='Request to ingest a BagIt resource',
        required=True
    )
})
