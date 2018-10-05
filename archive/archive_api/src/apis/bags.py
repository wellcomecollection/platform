# -*- encoding: utf-8

from flask import abort
from flask_restplus import Namespace, Resource, fields

from bags import fetch_bag, models
from error_handler import register_error_handler
from models import Error, register_models


api = Namespace("bags", description="Operations around BagIt bags")

register_models(api, models=models)
register_error_handler(api)


@api.route("/<id>")
@api.param("id", "The bag to return")
class BagResource(Resource):

    @api.doc(description="Returns a single bag")
    @api.marshal_with(models.Bag)
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", Error)
    def get(self, id):
        try:
            from archive_api import app
            return fetch_bag(
                dynamodb_resource=app.config["DYNAMODB_RESOURCE"],
                table_name=app.config["BAG_VHS_TABLE_NAME"],
                s3_client=app.config["S3_CLIENT"],
                bucket_name=app.config["BAG_VHS_BUCKET_NAME"],
                id=id,
            )
        except ValueError as error:
            abort(404, f"Invalid id: {error}")
