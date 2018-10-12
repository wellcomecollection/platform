# -*- encoding: utf-8

from flask import abort
from flask_restplus import Namespace, Resource

from models.bags import Bag, File, FileManifest, Source
from models.catalogue import Error, Identifier, IdentifierType
from storage import VHSError, VHSNotFound, read_from_vhs


api = Namespace("bags", description="Bag requests")

api.add_model("Bag", definition=Bag)
api.add_model("Error", definition=Error)
api.add_model("File", definition=File)
api.add_model("FileManifest", definition=FileManifest)
api.add_model("Identifier", definition=Identifier)
api.add_model("IdentifierType", definition=IdentifierType)
api.add_model("Source", definition=Source)


@api.route("/<space>/<id>")
@api.param("space", "The namespace in which objects get stored")
@api.param("id", "The bag to return")
class BagResource(Resource):
    @api.doc(description="Returns a single bag")
    # TODO: Replace this marshal_with
    # @api.marshal_with(Bag)
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", Error)
    def get(self, space, id):
        """Get the bag associated with an id"""
        from archive_api import app

        bag_id = f"{space}/{id}"

        try:
            result = read_from_vhs(
                dynamodb_resource=app.config["DYNAMODB_RESOURCE"],
                table_name=app.config["BAG_VHS_TABLE_NAME"],
                s3_client=app.config["S3_CLIENT"],
                bucket_name=app.config["BAG_VHS_BUCKET_NAME"],
                id=bag_id,
            )

            # TODO: Remove the necessity to do this
            result["id"] = result["id"]["value"]

            return result
        except VHSNotFound:
            abort(404, f"Invalid id: No bag found for id={bag_id!r}")
        except VHSError:
            abort(500)
