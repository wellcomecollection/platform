from flask import Flask
from flask_restful import Resource, Api
from utils import *

app = Flask(__name__)
api = Api(app)

# Load data
known_palette_distances = pd.read_pickle('data/palette_distances.pkl')
with open('data/palette_dict.pkl', 'rb') as f:
    palette_dict = pickle.load(f)

# Define classes
class most_similar(Resource):
    def get(self, query_id):
        most_similar_ids = (known_palette_distances[query_id]
                            .sort_values()
                            [1:11]
                            .index.values)

        return {'request': query_id,
                'response': ids_to_urls(most_similar_ids)}


class palette_search(Resource):
    def get(self, query_string):
        '''
        get top 10 most visually similar images to a supplied colour palette

        Parameters
        ----------
        query_string : string
            5 colour input palette as hex format string
            no #s, 5 colours of len 6, total len 30, no delimiters
            eg 'bbafa557534d383530726b63958b81'

        Returns
        -------
        most_similar_id_urls : list
            urls for the most similar images to the supplied palette 
        '''
        query_palette = [query_string[i : i + 6] for i in range(0, 30, 6)]
        query_palette = np.array([hex_to_rgb(colour) for colour in query_palette])

        rearranged = assignment_switch(query_palette, palette_dict)
        distance_values = vectorised_palette_distance(query_palette, rearranged)
        palette_distances = pd.Series(dict(zip(palette_dict.keys(), distance_values)))
        most_similar_ids = (palette_distances
                            .sort_values()
                            [1:11]
                            .index.values.tolist())

        return {'request': query_string,
                'response': ids_to_urls(most_similar_ids)}

# Define endpoints
api.add_resource(most_similar, '/most_similar/<string:query_id>')
api.add_resource(palette_search, '/palette_search/<string:query_string>')


if __name__ == '__main__':
    app.run(debug=True, host='0.0.0.0')