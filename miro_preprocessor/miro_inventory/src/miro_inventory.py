# -*- encoding: utf-8 -*-
"""
Lambda which passes on miro_image data from a SNS topic to an Elasticsearch cluster
"""

import json
import os

from botocore.vendored import requests


# Super naive jsonpath!
def _extract_id(o, id_field):
    path = id_field.split(".")

    for node in path:
        o = o[node]

    return o


def _extract_subject(event):
    return event['Records'][0]['Sns']['Subject']


def _extract_message(event):
    return json.loads(event['Records'][0]['Sns']['Message'])


def _generate_indexable_object(event, id_field):
    message = _extract_message(event)
    subject = _extract_subject(event)
    id = _extract_id(message, id_field)

    return {
        'id': id,
        'subject': subject,
        'message': message
    }


def _put_to_es(id, es_cluster_url, es_index, es_type, es_username, es_password, payload):
    put_url = f'{es_cluster_url}/{es_index}/{es_type}/{id}'
    print(f'PUTing to {put_url}')

    return requests.put(
        put_url,
        data=payload,
        auth=(es_username, es_password)
    )


def main(event, _):
    print(f'event = {event!r}')

    es_cluster_url = os.environ["ES_CLUSTER_URL"]
    es_index = os.environ["ES_INDEX"]
    es_type = os.environ["ES_TYPE"]

    es_username = os.environ["ES_USERNAME"]
    es_password = os.environ["ES_PASSWORD"]

    id_field = os.environ["ID_FIELD"]
    indexable = _generate_indexable_object(event, id_field)

    print(f'indexable_json: {indexable}')

    response = _put_to_es(
        indexable['id'],
        es_cluster_url,
        es_index,
        es_type,
        es_username,
        es_password,
        json.dumps(indexable)
    )
    print(f'_put_to_es response: {response}')
    response.raise_for_status()
