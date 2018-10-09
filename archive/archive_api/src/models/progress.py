# -*- encoding: utf-8
"""
Matches the models that come from the progress service.
"""

from flask_restplus import fields

from ._base import TypedModel


ProgressEvent = TypedModel(
    "ProgressEvent", {"description": fields.String(), "time": fields.String()}
)


Progress = TypedModel(
    "Progress",
    {
        "id": fields.String(),
        "uploadUri": fields.String(),
        "callbackUri": fields.String(),
        "result": fields.String(),
        "createdAt": fields.String(),
        "updatedAt": fields.String(),
        "events": fields.List(fields.Nested(ProgressEvent)),
    },
)
