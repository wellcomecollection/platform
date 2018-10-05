# -*- encoding: utf-8

from flask_restplus import Namespace, Resource, fields

from bags import models
from models import Error, register_models


api = Namespace("bags", description="Operations around BagIt bags")

register_models(api, models=models)


@api.route("/<id>")
@api.param("id", "The bag to return")
class BagResource(Resource):

    @api.doc(description="Returns a single bag")
    @api.response(200, "Bag found")
    @api.response(404, "Bag not found", Error)
    def get(self, id):
        return id
