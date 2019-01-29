import requests
import datetime
import settings
import json
from copy import deepcopy

storage_api_tokens = {}
ingest_template = {
    "type": "Ingest",
    "ingestType": {"id": "create", "type": "IngestType"},
    "space": {"id": "digitised", "type": "Space"},
    "sourceLocation": {
        "type": "Location",
        "provider": {"type": "Provider", "id": "aws-s3-standard"},
        "bucket": settings.DROP_BUCKET_NAME,
        "path": None,
    },
}


def get_api_token(scope):
    global storage_api_tokens
    scoped_token = storage_api_tokens.get(scope, None)
    if scoped_token is None or get_ttl(scoped_token) < 60:
        data = {
            "grant_type": "client_credentials",
            "client_id": settings.WELLCOME_API_CLIENT_ID,
            "client_secret": settings.WELLCOME_API_CLIENT_SECRET,
            "scope": scope,
        }
        resp = requests.post(settings.WELLCOME_API_TOKEN_ENDPOINT, data=data)
        token = resp.json()
        token["acquired"] = datetime.datetime.now()
        storage_api_tokens[scope] = token
    return storage_api_tokens[scope]


def get_ttl(token):
    expires = token["acquired"] + datetime.timedelta(seconds=token["expires_in"])
    td = expires - datetime.datetime.now()
    return td.total_seconds()


def get_oauthed_json(url, scope, method="GET", data=None):
    access_token = get_api_token(scope)["access_token"]
    headers = {"Authorization": "Bearer " + access_token}
    resp = requests.request(method, url, headers=headers, data=data)
    return resp.json()


def get_ingest_for_identifier(bnumber):
    scope = settings.STORAGE_API_INGESTS
    url = "{0}/find-by-bag-id/digitised:{1}".format(scope, bnumber)
    ingests = get_oauthed_json(url, scope)
    by_date = sorted(ingests, key=lambda ingest: ingest["createdDate"])
    if len(by_date) > 0:
        url = "{0}/{1}".format(scope, by_date[-1]["id"])
        return get_oauthed_json(url, scope)
    return None


def ingest(bnumber):
    global ingest_template
    body = deepcopy(ingest_template)
    body["sourceLocation"]["path"] = bnumber + ".zip"
    url = settings.STORAGE_API_INGESTS
    scope = settings.WELLCOME_API_SCOPE
    return get_oauthed_json(url or scope, scope, method="POST", data=json.dumps(body))
