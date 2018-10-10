# -*- encoding: utf-8
"""
Models used in the /ingests endpoint.
"""

from flask_restplus import fields

from ._base import TypedModel
from .progress import ProgressEvent

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
        )
    },
)

IngestStatus = TypedModel(
    "IngestStatus",
    {
        "id": fields.String(
            description="Identifier for ingest status",
            enum=["processing", "failure", "success"],
            required=True,
        )
    },
)

Ingest = TypedModel(
    "Ingest",
    {
        "id": fields.String(),
        "description": fields.String(),
        "uploadUrl": fields.String(
            attribute="uploadUri",
            description="S3 URL of uploaded BagIt resource, supports only a zipped BagIt file",
            example="s3://source-bucket/source-path/source-bag.zip",
            required=True,
        ),
        "callbackUrl": fields.String(
            attribute="callbackUri",
            description="URL to use for callback on completion or failure",
            example="https://workflow.wellcomecollection.org/callback?id=b1234567",
        ),
        "ingestType": fields.Nested(
            IngestType,
            description="Type of request to ingest a BagIt resource",
            required=True,
        ),
        # "status": fields.Nested(
        #     IngestStatus,
        #     description="Status of ingest processing",
        #     required=True
        # ),
        "createdDate": fields.String(attribute="createdAt"),
        "lastModifiedDate": fields.String(attribute="updatedAt"),
        "events": fields.List(fields.Nested(ProgressEvent)),
    },
)
