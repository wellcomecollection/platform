# -*- encoding: utf-8

import types

from flask_restplus import fields, Model


class TypedModel(Model):
    """
    A thin wrapper around ``Model`` that adds a ``type`` field.
    """

    def __init__(self, name, model_fields, *args, **kwargs):

        # When you use a model in ``@api.response``, it triggers an internal
        # cloning operation, which passes the ``model_fields`` parameter as
        # a list of 2-tuples.
        #
        # In that case, we should already have a ``type`` field, so we
        # can skip adding it.
        #
        if isinstance(model_fields, dict) and "type" not in model_fields:
            model_fields["type"] = fields.String(
                description="Type of the object",
                enum=[name]
            )

        super().__init__(name, model_fields, *args, **kwargs)


def register_models(namespace, models):
    """
    Register all the models in a module against the given namespace.

    We have to do this because Flask-RESTPlus doesn't register models
    recursively -- so if we have Foo(id: str, bar: Bar), then registering
    the Foo model doesn't get us the Bar model, and then the Swagger
    fails to render.  Boo.
    """
    # How it works: for a module, the __dict__ contains a mapping from
    # name to importable object.  By iterating over the values, we get
    # everything that you can import from the module.
    #
    assert isinstance(models, types.ModuleType)
    for m in models.__dict__.values():
        if not isinstance(m, TypedModel):
            continue

        # This function gets called recursively (somewhere) -- this stops
        # us triggering an infinite recursion.
        if m.name not in namespace.models:
            namespace.add_model(m.name, m)


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
    "Ingest type",
    {
        "id": fields.String(
            description="Identifier for ingest type", enum=["create"], required=True
        ),
    },
)

IngestRequest = TypedModel(
    "Ingest request",
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
Error = TypedModel(
    "Error",
    {
        "errorType": fields.String(description="The type of error", enum=["http"]),
        "httpStatus": fields.Integer(description="The HTTP response status code"),
        "label": fields.String(
            description="The title or other short name of the error"
        ),
        "description": fields.String(description="The specific error"),
    },
)
