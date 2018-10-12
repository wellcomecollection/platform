# -*- encoding: utf-8

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
                enum=[name],
                default=name,
                required=True,
            )

        super().__init__(name, model_fields, *args, **kwargs)
