# -*- encoding: utf-8
"""
Implements the "IngestRequest" model, as required by the POST /ingests endpoint.

This is an example of a valid request to that endpoint, based on the RFC
at commit 8e2a483:

    {
      "type": "Ingest",
      "ingestType": {
        "id": "create",
        "type": "IngestType"
      },
      "uploadUrl": "s3://source-bucket/source-path/source-bag.zip",
      "callbackUrl": "https://example.org/callback?id=b1234567",
    }

"""

from flask_restplus import fields

from models import TypedModel


IngestType = TypedModel(
    "IngestType",
    {
        "id": fields.String(
            description="Identifier for ingest type", enum=["create"], required=True
        )
    },
)

IngestRequest = TypedModel(
    "Ingest",
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
