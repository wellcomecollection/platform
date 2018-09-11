# -*- encoding: utf-8

from flask import Flask

import config


app = Flask(__name__)
app.config.from_object('config')

from views import *


@app.route('/')
def hello_world():
    return 'Hello world'
