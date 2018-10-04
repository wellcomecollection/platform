from flask import Flask, jsonify
from flask_restful import Resource, Api
from flask_cors import CORS
import numpy as np
import pickle

app = Flask(__name__)
api = Api(app)
cors = CORS(app)

# Load data
with open("../data/image_ids_palettes.npy", "rb") as f:
    image_ids_palettes = np.load(f).tolist()

with open("../data/image_ids_features.npy", "rb") as f:
    image_ids_features = np.load(f).tolist()

with open("../data/nearest_neighbours_palettes.pkl", "rb") as f:
    palette_values = pickle.load(f).tolist()

with open("../data/nearest_neighbours_features.pkl", "rb") as f:
    feature_values = pickle.load(f).tolist()

with open("../data/all_palettes.pkl", "rb") as f:
    all_palettes = pickle.load(f).tolist()


# Assemble dictionaries
palette_dict = dict(zip(image_ids_palettes, all_palettes))
palette_similarity_dict = dict(zip(image_ids_palettes, palette_values))
feature_similarity_dict = dict(zip(image_ids_features, feature_values))


# Define classes
class palette(Resource):
    def get(self, query_id):
        """
        return the colour palette of the query image as a numpy array of shape
        (5, 3), corresponding to 5 3-channel (RGB) colours
        """
        return jsonify({"request": query_id, "response": palette_dict[query_id]})


class palette_similarity(Resource):
    def get(self, query_id):
        """
        return the ids of the 10 most similar images to the query, based on their
        colour palettes
        """
        return jsonify(
            {"request": query_id, "response": palette_similarity_dict[query_id]}
        )


class feature_similarity(Resource):
    def get(self, query_id):
        """
        return the ids of the 10 most similar images to the query, based on their
        visual features
        """
        return jsonify(
            {"request": query_id, "response": feature_similarity_dict[query_id]}
        )


class health_check(Resource):
    def get(self):
        return jsonify({"status": "healthy"})


# Define endpoints
api.add_resource(health_check, "/image_similarity/health_check")
api.add_resource(palette, "/image_similarity/api/palette/<string:query_id>")
api.add_resource(
    palette_similarity, "/image_similarity/api/palette_similarity/<string:query_id>"
)
api.add_resource(
    feature_similarity, "/image_similarity/api/feature_similarity/<string:query_id>"
)


if __name__ == "__main__":
    app.run(debug=True, host="0.0.0.0", port=80)
