# -*- encoding: utf-8
"""
Models used in the /ingests endpoint.
"""

from flask_restplus import fields

from ._base import TypedModel


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
IngestType = TypedModel(
    "IngestType",
    {
        "id": fields.String(
            description="Identifier for ingest type", enum=["create"], required=True
        ),
    },
)

IngestRequest = TypedModel(
    "IngestRequest",
    {
        "uploadUrl": fields.String(
            description="S3 URL of uploaded BagIt resource, supports only a zipped BagIt file",
            example="s3://source-bucket/source-path/source-bag.zip",
            required=True,
        ),
        "callbackUrl": fields.String(
            description="URL to use for callback on completion or failure",
            example="https://workflow.wellcomecollection.org/callback?id=b1234567",
        ),
        "ingestType": fields.Nested(
            IngestType, description="Request to ingest a BagIt resource", required=True
        ),
    },
)
