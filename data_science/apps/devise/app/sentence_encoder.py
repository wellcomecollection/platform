import torch
from torch import nn


# sentence embedding models
class SentenceEncoder(nn.Module):
    def __init__(self):
        super(SentenceEncoder, self).__init__()
        self.enc_lstm = nn.LSTM(
            input_size=300, hidden_size=2048, num_layers=1, bidirectional=True
        )

    def forward(self, wv_batch):
        embedded, _ = self.enc_lstm(wv_batch)
        max_pooled = torch.max(embedded, 1)[0]
        return max_pooled
