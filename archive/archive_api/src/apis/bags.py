# -*- encoding: utf-8

from flask import abort, jsonify
from flask_restplus import Namespace, Resource

from models import Error
from storage import VHSNotFound, read_from_vhs


api = Namespace("bags", description="Bag requests")


@api.route("/<id>")
@api.param("id", "The bag identifier")
class BagResource(Resource):
    @api.doc(description="The bag is returned in the body of the response")
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", Error)
    def get(self, id):
        """Get the bag associated with an id"""
        from archive_api import app
        try:
            result = read_from_vhs(
                dynamodb_resource=app.config["DYNAMODB_RESOURCE"],
                table_name=app.config["BAG_VHS_TABLE_NAME"],
                s3_client=app.config["S3_CLIENT"],
                bucket_name=app.config["BAG_VHS_BUCKET_NAME"],
                id=id,
            )
            return jsonify(result)
        except VHSNotFound:
            abort(404, f"Invalid id: No bag found for id={id!r}")
