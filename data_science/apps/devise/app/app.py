from flask import Flask, jsonify, redirect, send_from_directory, request
from flask_restful import Resource, Api
from flask_cors import CORS

import urllib
import nmslib
import torch
import numpy as np
import pickle
from sentence_encoder import SentenceEncoder
from utils import search

# Set up basic app
app = Flask(__name__)
api = Api(app)
cors = CORS(app)

# Load required data
image_ids = np.load('../data/image_ids.npy')
index_to_wordvec = np.load('../data/index_to_wordvec.npy')
word_to_index = pickle.load(open('../data/word_to_index.pkl', 'rb'))

# Create sentence embedding model and load its pre-trained weights
model = SentenceEncoder()
model.load_state_dict(torch.load('../data/sentence-encoder-2018-10-08.pt',
                                 map_location='cpu'))

# Initialise a search index based on the images' devise'd sentence embeddings.
# See notebooks for a complete explanation of how/why we're searching on these
# embeddings and what they represent.
search_index = nmslib.init(method='hnsw', space='cosinesimil')
search_index.loadIndex('../data/search_index.hnsw')

# Define endpoint classes
class devise_search(Resource):
    def get(self):
        query = urllib.parse.unquote_plus(request.args.get('query'))
        n = int(request.args.get('n'))

        response_ids = search(query, search_index, model, image_ids, 
                              word_to_index, index_to_wordvec, n)
        blank_url = 'https://iiif.wellcomecollection.org/image/{}.jpg/full/760,/0/default.jpg'
        response_urls = [blank_url.format(id) for id in response_ids]

        return jsonify({'request': query,
                        'response': response_urls})

# Create search endpoint
api.add_resource(devise_search, '/devise/search')

# Routing
@app.route("/devise")
def index():
    return redirect('/devise/index.html')

@app.route('/devise/<path:path>')
def send_index(path):
    return send_from_directory('static', path)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=80)
    
