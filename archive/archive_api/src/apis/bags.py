# -*- encoding: utf-8

from flask import abort
from flask_restplus import Namespace, Resource

from bags import models as bag_models
from models import Error, register_models
from storage import VHSError, VHSNotFound, read_from_vhs


api = Namespace("bags", description="Operations around BagIt bags")

register_models(api, models=bag_models)


@api.route("/<id>")
@api.param("id", "The bag to return")
class BagResource(Resource):
    @api.doc(description="Returns a single bag")
    @api.marshal_with(bag_models.Bag)
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", Error)
    def get(self, id):
        try:
            from archive_api import app

            return read_from_vhs(
                dynamodb_resource=app.config["DYNAMODB_RESOURCE"],
                table_name=app.config["BAG_VHS_TABLE_NAME"],
                s3_client=app.config["S3_CLIENT"],
                bucket_name=app.config["BAG_VHS_BUCKET_NAME"],
                id=id,
            )
        except VHSNotFound:
            abort(404, f"No bag found for id={id!r}")
        except VHSError:
            abort(500)
