# -*- encoding: utf-8 -*-
"""
Description
"""

import base64
import json
import os

from botocore.vendored import requests

# Super naive jsonpath!
def _extract_id_field(o, id_field):
    path = id_field.split(".")

    for node in path:
        o = o[node]

    return o


def _generate_indexable_json(event, id_field):
    subject = event['Records'][0]['Sns']['Subject']
    message = json.loads(event['Records'][0]['Sns']['Message'])
    id = _extract_id_field(message, id_field)

    return json.dumps({
        'id': id,
        'subject': subject,
        'message': message
    })


def _generate_auth_token(username, password):
    plaintext = f'{username}:{password}'
    return base64.b64encode(plaintext.encode())


def _post_to_es(es_cluster_url, es_index, es_type, es_username, es_password, payload):
    token = _generate_auth_token(es_username, es_password)
    post_url = f'{es_cluster_url}/{es_index}/{es_type}'

    print(f'POSTing to {post_url}')

    ascii_token = token.decode('ascii')

    return requests.post(
        post_url,
        data=payload,
        headers={'Authorization': f'Basic {ascii_token}'}
    )


def main(event, _):
    print(f'event = {event!r}')

    es_cluster_url = os.environ["ES_CLUSTER_URL"]
    es_index = os.environ["ES_INDEX"]
    es_type = os.environ["ES_TYPE"]

    es_username = os.environ["ES_USERNAME"]
    es_password = os.environ["ES_PASSWORD"]

    id_field = os.environ["ID_FIELD"]

    indexable_json = _generate_indexable_json(event, id_field)
    print(f'indexable_json: {indexable_json}')

    response = _post_to_es(
        es_cluster_url,
        es_index,
        es_type,
        es_username,
        es_password,
        indexable_json
    )
    print(f'_post_to_es response: {response}')
    response.raise_for_status()
