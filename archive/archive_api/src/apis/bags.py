# -*- encoding: utf-8

from flask import abort
from flask_restplus import Namespace, Resource

from models.bags import Bag, File, FileManifest, Source
from models.catalogue import Error, Identifier, IdentifierType
from bags_manager import BagNotFoundError


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

        bags_manager = app.config["BAGS_MANAGER"]

        try:
            result = bags_manager.lookup_bag(space, id)

            # TODO: Remove the necessity to do this
            result["id"] = f'{result["id"]["space"]}/{result["id"]["externalIdentifier"]}'

            return result
        except BagNotFoundError:
            bag_id = f"{space}/{id}"
            abort(404, f"Invalid id: No bag found for id={bag_id!r}")
