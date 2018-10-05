# -*- encoding: utf-8

from flask_restplus import Namespace, Resource, fields

from bags import Bag
from models import Error


api = Namespace("bags", description="Operations around BagIt bags")


@api.route("/<id>")
@api.param("id", "The bag to return")
class BagResource(Resource):

    @api.doc(description="Returns a single bag")
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", models.Error)
    def get(self, id):
        return id
