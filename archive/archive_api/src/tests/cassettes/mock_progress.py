#!/usr/bin/env python
# -*- encoding: utf-8
"""
This is a dummy version of the progress app, which was used to create
the responses for the ProgressManager tests.

It mimics the external interface but not the functionlity!
"""

from flask import Flask, jsonify, request, Response
import uuid

app = Flask(__name__)


@app.route("/progress", methods=["POST"])
def post_route():
    # e.g. http://localhost:6000?status=500
    if "?status=" in request.form["uploadUrl"]:
        return b"", int(request.form["uploadUrl"].split("=")[1])

    # e.g. http://localhost:6000?location=no
    if request.form["uploadUrl"].endswith("?location=no"):
        return b"", 201

    # e.g. http://localhost:6000?id=123
    # This lets an external caller pick a deterministic ID.
    if "?id=" in request.form["uploadUrl"]:
        new_id = request.form["uploadUrl"].split("?id=")[1]
    else:
        new_id = str(uuid.uuid4())

    resp = Response()
    resp.headers["Location"] = f"/progress/{new_id}"
    return resp, 201


@app.route("/progress/<id>")
def get_route(id):
    if id.startswith("bad_status-"):
        return b"", int(id.split("bad_status-")[1])
    elif id == "notjson":
        return b"<<notjson>>"
    return jsonify({"progress": id})


app.run(debug=True, port=6000)
