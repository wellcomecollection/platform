import torch
import spacy
import pickle
from bs4 import BeautifulSoup
from model import LinkLabeller
from tokenise import prepare_model_inputs

ix_to_token = pickle.load(open("../data/ix_to_token.pkl", "rb"))
unique_characters = pickle.load(open("../data/unique_characters.pkl", "rb"))
embedding_matrix = torch.load(
    "../data/embedding_matrix.pt",
    map_location=torch.device("cpu")
)

model = LinkLabeller(
    unique_characters=unique_characters,
    word_vector_embedding_matrix=embedding_matrix
)

model.load_state_dict(torch.load(
    "../data/model_state_dict.pt",
    map_location=torch.device("cpu")
))


def extract_entities(document):
    """
    return the entities in the input text
    """
    clean_text = BeautifulSoup(document, features="html.parser").text
    model_inputs = prepare_model_inputs(clean_text)
    entities = model.find_entities(*model_inputs, ix_to_token)
    return entities
