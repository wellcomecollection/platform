from flask import Flask, jsonify, redirect, send_from_directory
from flask_restful import Resource, Api
from utils import *
from flask_cors import CORS
import pandas as pd


app = Flask(__name__)
api = Api(app)
cors = CORS(app)

# Load data
known_palette_distances = pd.read_pickle('../data/palette_distances.pkl')
with open('../data/palette_dict.pkl', 'rb') as f:
    palette_dict = pickle.load(f)


# Define classes
class most_similar(Resource):
    def get(self, query_id):
        most_similar_ids = (known_palette_distances[query_id]
                            .sort_values()
                            [1:13]
                            .index.values)

        return jsonify({'request': query_id,
                        'response': ids_to_urls(most_similar_ids)})


class palette_search(Resource):
    def get(self, query_string):
        """
        query_string : string
            5 colour input palette as hex format string
            no #s, 5 colours of len  6, total len 30, no delimiters
            eg 'bbafa557534d383530726b63958b81'
        """
        query_palette = [query_string[i : i + 6] for i in range(0, 30, 6)]
        rgb_palette = np.array([hex_to_rgb(colour) for colour in query_palette])
        palette = rgb_to_lab(rgb_palette)

        rearranged = assignment_switch(palette, palette_dict)
        distance_values = vectorised_palette_distance(palette, rearranged)
        palette_distances = pd.Series(dict(zip(palette_dict.keys(), distance_values)))
        most_similar_ids = (palette_distances
                            .sort_values()
                            [1:13]
                            .index.values.tolist())

        return jsonify({'request': query_palette,
                        'response': ids_to_urls(most_similar_ids)})


@app.route("/palette")
def index():
    return redirect('/palette/index.html')


@app.route('/palette/<path:path>')
def send_index(path):
    return send_from_directory('static', path)

# Define endpoints
api.add_resource(most_similar, '/api/most_similar/<string:query_id>')
api.add_resource(palette_search, '/api/palette_search/<string:query_string>')


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0', port=80)