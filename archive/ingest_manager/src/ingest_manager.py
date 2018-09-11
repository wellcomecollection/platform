# -*- encoding: utf-8

from flask import Flask

app = Flask(__name__)

import config
app.config.from_object('config')

from views import *


@app.route('/')
def hello_world():
    return 'Hello world'
