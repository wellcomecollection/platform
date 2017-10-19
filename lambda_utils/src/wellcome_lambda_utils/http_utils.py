# -*- encoding: utf-8 -*-

import requests


def retry(method, *args, **kwargs):
    """
    Sometimes the HTTP APIs can be a little flakey.  This function wraps
    a method from ``requests``, and retries it once if it fails the first time.
    """
    resp = method(*args, **kwargs)
    try:
        resp.raise_for_status()
    except Exception:
        time.sleep(1)
    else:
        return resp

    resp = method(*args, **kwargs)
    resp.raise_for_status()
    return resp


def http_post(*args, **kwargs):
    return retry(requests.post, *args, **kwargs)
