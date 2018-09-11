# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask

import config


app = Flask(__name__)
app.config.from_object('config')

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()

from views import *


@app.route('/')
def hello_world():
    return 'Hello world'
