# -*- encoding: utf-8

from flask_restplus import Model, fields


def fieldType(name, **kwargs):
    return fields.String(description="Type of the object", enum=[name], default=name, **kwargs)


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
IngestType = Model(
    "IngestType",
    {
        "type": fieldType(name="IngestType", required=True),
        "id": fields.String(
            description="Identifier for ingest type", enum=["create"], required=True
        ),
    },
)

IngestRequest = Model(
    "IngestRequest",
    {
        "type": fieldType(name="Ingest", required=True),
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


# Example of an error in the Catalogue API style:
#
#       {
#         "@context:" "https://example.org/catalogue/v2/context.json",
#         "errorType:" "http",
#         "httpStatus:" 404,
#         "label:" "Not Found",
#         "description:" "Work not found for identifier 1234",
#         "type:" "Error"
#       }
#
# TODO: It would be much better if we could define this model in a common
# location, rather than copying this from the Scala app Swagger spec.
#
Error = Model(
    "Error",
    {
        "errorType": fields.String(description="The type of error", enum=["http"], default="http"),
        "httpStatus": fields.Integer(description="The HTTP response status code"),
        "label": fields.String(
            description="The title or other short name of the error"
        ),
        "description": fields.String(description="The specific error"),
        "type": fieldType(name="Error"),
    },
)
