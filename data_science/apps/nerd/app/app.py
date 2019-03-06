from flask import Flask, jsonify
from flask_restful import Resource, Api
from flask_cors import CORS

from extract import extract_entities
from disambiguate import disambiguate, annotate_with_links

app = Flask(__name__)
api = Api(app)
cors = CORS(app)


class annotate(Resource):
    def get(self, query_text):
        """
        annotate plain text with links to wikipedia pages
        """
        return jsonify({
            "request": query_text,
            "response": annotate_with_links(query_text)
        })


class entities(Resource):
    def get(self, query_text):
        """
        returns a list of entities found in the text
        """
        return jsonify({
            "request": query_text,
            "response": extract_entities(query_text)
        })


class entities_with_links(Resource):
    def get(self, query_text):
        """
        returns a dict of entities found in the text with their corresponding
        wikipedia urls
        """
        entities = extract_entities(query_text)
        wikipedia_pages = disambiguate(entities)
        return jsonify({
            "request": query_text,
            "response": wikipedia_pages
        })


class health_check(Resource):
    def get(self):
        return jsonify({"status": "healthy"})


# Define endpoints
api.add_resource(health_check, "/nerd/health_check")
api.add_resource(annotate, "/nerd/annotate/<string:query_text>")
api.add_resource(entities, "/nerd/entities/<string:query_text>")
api.add_resource(entities_with_links,
                 "/nerd/entities_with_links/<string:query_text>")

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=80)
