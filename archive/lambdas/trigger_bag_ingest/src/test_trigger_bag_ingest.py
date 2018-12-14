# -*- encoding: utf-8 -*-

import os
import requests
import datetime

wiremock_server = "localhost:8080"
os.environ["OAUTH_DETAILS_ENC"] = ""
os.environ["BAG_PATHS"] = "b123456.zip"
os.environ["STORAGE_SPACE"] = "space"
os.environ["INGESTS_BUCKET"] = "wellcome-ingests-bucket"
os.environ["API_URL"] = f"http://{wiremock_server}/"


import trigger_bag_ingest  # noqa: E402


def test_lambda(storage_client):
    start_time = datetime.datetime.now().isoformat()
    trigger_bag_ingest.main(event=None, context=None)

    r = requests.get(f"http://{wiremock_server}/__admin/requests?since={start_time}Z")
    assert r.status_code == 200
    ingest_requests = r.json()["requests"]
    assert len(ingest_requests) == 1
