#!/usr/bin/env python
# -*- encoding: utf-8

import functools
import json
import multiprocessing

from flask import Flask


app = Flask(__name__)

@app.route('/management/healthcheck')
def healthcheck():
    return json.dumps({'ok': True})


def service(handler):

    @functools.wraps(handler)
    def wrapper():
        proc = multiprocessing.Process(
            target=app.run,
            kwargs={'host': '0.0.0.0', 'port': 9000},
            daemon=True
        )
        proc.start()
        handler()

    return wrapper
