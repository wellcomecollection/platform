# -*- encoding: utf-8
"""
Models that correspond to models in the Catalogue API.
"""

from flask_restplus import fields

from ._base import TypedModel


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
        "errorType": fields.String(
            description="The type of error", enum=["http"], default="http"
        ),
        "httpStatus": fields.Integer(description="The HTTP response status code"),
        "label": fields.String(
            description="The title or other short name of the error"
        ),
        "description": fields.String(description="The specific error"),
    },
)


IdentifierType = TypedModel(
    "IdentifierType", {"id": fields.String(), "label": fields.String()}
)


Identifier = TypedModel(
    "Identifier",
    {
        "identifierType": fields.Nested(
            IdentifierType,
            description="Relates a Identifier to a particular authoritative source identifier scheme: for example, if the identifier is MS.49 this property might indicate that this identifier has its origins in the Wellcome Library's CALM archive management system.",
        ),
        "value": fields.String(
            description="The value of the thing. e.g. an identifier"
        ),
    },
)
