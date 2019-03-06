import torch
import pickle
import numpy as np
from nltk import word_tokenize

token_to_ix = pickle.load(open("../data/token_to_ix.pkl", "rb"))
char_to_ix = pickle.load(open("../data/char_to_ix.pkl", "rb"))
vocabulary = pickle.load(open("../data/article_vocabulary.pkl", "rb"))


def tokenize(sentence):
    """moses tokeniser"""
    seq = " ".join(word_tokenize(sentence))
    seq = seq.replace(" n't ", "n 't ")
    return seq.split()


def find_exit_points(char_ix_seq):
    binary = (char_ix_seq == char_to_ix[" "])
    return binary.nonzero().squeeze()


def prepare_model_inputs(clean_text):
    """
    takes clean text and tokenises it into words and characters, ready to be
    processed by the linklabeller model
    """
    token_seq = tokenize(clean_text)

    t_seq = torch.LongTensor(np.array([[
        token_to_ix[token]
        if token in vocabulary
        else token_to_ix["xxunk"]
        for token in token_seq
    ]]))

    c_seq = torch.LongTensor(np.array([
        [char_to_ix["xxbos"], char_to_ix[" "]] +
        [char_to_ix[char]
         if char in char_to_ix else char_to_ix["xxunk"]
         for char in " ".join(token_seq)] +
        [char_to_ix[" "], char_to_ix["xxeos"]]
    ]))

    exit_seq = find_exit_points(c_seq[0]).unsqueeze(0)
    c_len = torch.LongTensor([len(c_seq[0])])
    t_len = torch.LongTensor([len(t_seq[0])])

    return c_seq, t_seq, exit_seq, c_len, t_len
