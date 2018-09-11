# -*- encoding: utf-8

from flask import Flask

import config


app = Flask(__name__)
app.config.from_object('config')

from views import *

@app.route('/')
def hello_world():
    return 'Hello world'



if __name__ == '__main__':
    app.run(host='0.0.0.0')
