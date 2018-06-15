import numpy as np
import pandas as pd
import pickle
from scipy.spatial.distance import cdist
from scipy.optimize import linear_sum_assignment


def colour_distance(colour_1, colour_2):
    return sum([(a - b) ** 2 for a, b in zip(colour_1, colour_2)]) ** 0.5


def assignment_switch(query_palette, palette_dict):
    rearranged = []
    for other_palette in palette_dict.values():
        distances = [[colour_distance(c1, c2)
                      for c2 in other_palette]
                     for c1 in query_palette]
        
        _, rearrangement = linear_sum_assignment(distances)
        rearranged.append([other_palette[i] for i in rearrangement])

    return np.array(rearranged)


def vectorised_palette_distance(query_palette, rearranged):
    query = query_palette.reshape(-1, 1, 3)
    palettes = [p.squeeze() for p in np.split(rearranged, 5, axis=1)]

    colour_distances = np.stack([cdist(q, p, metric='cosine') 
                                 for q, p in zip(query, palettes)])
    
    palette_distances = np.sum(colour_distances.squeeze(), axis=0)
    return palette_distances


def hex_to_rgb(hex):
     return [int(hex[i : i + 2], 16) for i in range(0, 6, 2)]


def ids_to_urls(image_ids):
    url = 'https://iiif.wellcomecollection.org/image/{}/full/760,/0/default.jpg'
    return [url.format(image_id) for image_id in image_ids]