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
#       "space": {
#         "id": "space-id",
#         "type": "Space"
#       },
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

Space = TypedModel(
    "Space", {"id": fields.String(description="Identifier for space", required=True)}
)

Callback = TypedModel("Callback", {"uri": fields.String(), "status": fields.String()})

IngestResource = TypedModel("IngestResource", {"id": fields.String()})

Ingest = TypedModel(
    "Ingest",
    {
        "id": fields.String(),
        "uploadUrl": fields.String(
            description="S3 URL of uploaded BagIt resource, supports only a zipped BagIt file",
            example="s3://source-bucket/source-path/source-bag.zip",
            required=True,
        ),
        "callback": fields.Nested(
            Callback, description="Callback details used on completion or failure"
        ),
        "resources": fields.List(
            fields.Nested(IngestResource, description="Bag resources")
        ),
        "ingestType": fields.Nested(
            IngestType,
            description="Type of request to ingest a BagIt resource",
            required=True,
        ),
        "space": fields.Nested(
            Space, description="Name of the space in which to store Bag", required=True
        ),
        "status": fields.Nested(
            IngestStatus, description="Status of ingest processing"
        ),
        "createdDate": fields.String(),
        "lastModifiedDate": fields.String(),
        "events": fields.List(fields.Nested(ProgressEvent)),
    },
)
