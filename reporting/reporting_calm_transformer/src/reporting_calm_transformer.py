import pandas as pd
from transform import transform
from tqdm import tqdm_notebook as tqdm
from elasticsearch import Elasticsearch

es_username = ''
es_password = ''
es_url = ''
path_to_calm_json = ''

df = pd.read_json(path_to_calm_json)
es = Elasticsearch(es_url, http_auth=(es_username, es_password))

for idx in tqdm(range(len(df))):
    try:
        record = df.iloc[idx].to_dict()
        record = transform(record)
        res = es.index(
            index='calm',
            id=record['RecordID'],
            doc_type='calm_record',
            body=record,
        )
    except Exception as e:
        print(e)
