import torch
from torch import nn
from torch.nn.utils.rnn import pack_padded_sequence, pad_packed_sequence

import numpy as np
from more_itertools import consecutive_groups


class CharacterLevelNetwork(nn.Module):
    def __init__(self, input_dim, embedding_dim, hidden_dim, output_dim):
        super(CharacterLevelNetwork, self).__init__()
        self.output_dim = output_dim
        self.embedding = nn.Embedding(input_dim, embedding_dim)

        self.char_level_lstm = nn.LSTM(
            input_size=embedding_dim,
            hidden_size=hidden_dim,
            num_layers=1,
            bidirectional=True,
        )

        self.head_fwd = nn.Sequential(
            nn.Dropout(0.3),
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(hidden_dim // 2, output_dim),
        )

        self.head_bwd = nn.Sequential(
            nn.Dropout(0.3),
            nn.Linear(hidden_dim, hidden_dim // 2),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(hidden_dim // 2, output_dim),
        )

    def forward(self, char_seqs, exit_seqs, lengths):
        x = self.embedding(char_seqs)

        x = pack_padded_sequence(x, lengths=lengths, batch_first=True)

        x, _ = self.char_level_lstm(x)
        out, _ = pad_packed_sequence(x, batch_first=True)

        # pop out the character embeddings at position of the end of each token
        out = torch.stack([out[i, exit_seqs[i]] for i in range(len(out))])

        out_fwd, out_bwd = torch.chunk(out, 2, 2)

        pred_fwd = self.head_fwd(out_fwd[:, 1:])
        pred_bwd = self.head_bwd(out_bwd[:, :-1])

        return pred_fwd, pred_bwd


class LinkLabeller(nn.Module):
    def __init__(self, unique_characters, word_vector_embedding_matrix, hidden_dim=1024):
        super(LinkLabeller, self).__init__()
        self.wv_embedding = nn.Embedding.from_pretrained(
            word_vector_embedding_matrix)

        self.cln = CharacterLevelNetwork(
            input_dim=len(unique_characters),
            embedding_dim=50,
            hidden_dim=128,
            output_dim=50
        )

        self.lstm_input_size = (
            word_vector_embedding_matrix.shape[1] +
            (self.cln.output_dim * 2)
        )

        self.word_level_lstm = nn.LSTM(
            input_size=self.lstm_input_size,
            hidden_size=hidden_dim,
            num_layers=2,
            bidirectional=True,
            dropout=0.2
        )

        self.head = nn.Sequential(
            nn.Dropout(0.3),
            nn.Linear(hidden_dim * 2, hidden_dim // 32),
            nn.ReLU(),
            nn.Dropout(0.3),
            nn.Linear(hidden_dim // 32, 2),
        )

    def forward(self, char_seqs, token_seqs, exit_seqs, c_lens, t_lens):
        wv_seqs = self.wv_embedding(token_seqs)
        char_fwd, char_bwd = self.cln(char_seqs, exit_seqs, c_lens)

        concats = torch.cat(
            [char_fwd, char_bwd, wv_seqs],
            dim=2
        )

        sorted_lengths, sort_indicies = t_lens.sort(
            dim=0,
            descending=True
        )

        concats = torch.stack([concats[i] for i in sort_indicies])

        packed = pack_padded_sequence(
            concats,
            lengths=sorted_lengths,
            batch_first=True
        )

        packed_embedded, _ = self.word_level_lstm(packed)
        embedded, _ = pad_packed_sequence(packed_embedded)

        output = self.head(embedded).permute(1, 2, 0)
        return output, sort_indicies

    def find_entities(self, char_seq, token_seq, exit_seq, c_len, t_len, ix_to_token):
        preds, _ = self.forward(char_seq, token_seq, exit_seq, c_len, t_len)
        preds = nn.LogSoftmax(dim=1)(preds).argmax(dim=1).numpy()[0]
        tokens = [ix_to_token[token.item()] for token in token_seq[0]]
        labelled_indexes = np.where(preds == 1)[0].tolist()
        runs = [list(group) for group in consecutive_groups(labelled_indexes)]
        entities = [" ".join([tokens[ix] for ix in run]) for run in runs]
        unique_entities = list(set(entities))
        return unique_entities
