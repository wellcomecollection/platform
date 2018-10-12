import torch
from torch import nn


# sentence embedding models
class SentenceEncoder(nn.Module):
    def __init__(self, index_to_wordvec):
        super(SentenceEncoder, self).__init__()
        self.index_to_wordvec = torch.FloatTensor(index_to_wordvec)
        self.enc_lstm = nn.LSTM(input_size=300,
                                hidden_size=512,
                                num_layers=1,
                                bidirectional=True)

    def forward(self, wv_batch):
        embedded, _ = self.enc_lstm(wv_batch)
        max_pooled = torch.max(embedded, 1)[0]
        return max_pooled
