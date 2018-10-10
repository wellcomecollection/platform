import torch
import numpy as np
from nltk.tokenize import word_tokenize


def sentence_to_indexes(sentence, word_to_index):
    tokenised = word_tokenize(sentence)
    indexes = [word_to_index[word] for word in tokenised if word in word_to_index]
    return indexes


def embed(sentence, model, word_to_index, index_to_wordvec):
    indexes = (
        [word_to_index["<s>"]]
        + sentence_to_indexes(sentence, word_to_index)
        + [word_to_index["</s>"]]
    )
    wvs = np.stack([index_to_wordvec[i] for i in indexes])
    embedding = model(torch.Tensor([wvs])).detach().numpy()
    return embedding.squeeze()


def search(
    query_string, search_index, model, image_ids, word_to_index, index_to_wordvec, k=10
):
    query_embedding = embed(query_string, model, word_to_index, index_to_wordvec)
    neighbour_indexes, _ = search_index.knnQuery(query_embedding, k)
    return image_ids[neighbour_indexes]
