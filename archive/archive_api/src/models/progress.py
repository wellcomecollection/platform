# -*- encoding: utf-8
"""
Matches the models that come from the progress service.
"""

from flask_restplus import fields

from ._base import TypedModel


ProgressEvent = TypedModel(
    "ProgressEvent", {"description": fields.String(), "createdDate": fields.String()}
)

Callback = TypedModel(
    "Callback", {"uri": fields.String(), "status": fields.String()}
)

Space = TypedModel(
    "Space", {"id": fields.String(description="Identifier for space", required=True)}
)

Progress = TypedModel(
    "Progress",
    {
        "id": fields.String(),
        "uploadUri": fields.String(),
        "callback": fields.Nested(Callback),
        "space": fields.String(),
        "result": fields.String(),
        "createdAt": fields.String(),
        "updatedAt": fields.String(),
        "events": fields.List(fields.Nested(ProgressEvent)),
    },
)
