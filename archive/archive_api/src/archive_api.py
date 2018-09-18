# -*- encoding: utf-8

import os

import daiquiri
from flask import Flask



app = Flask(__name__)
app.config.from_object('config')

daiquiri.setup(level=os.environ.get('LOG_LEVEL', 'INFO'))
logger = daiquiri.getLogger()

