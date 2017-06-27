#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from loris.webapp import create_app

# application = create_app(
# 	debug=False,
# 	config_file_path='/opt/loris/etc/loris2.conf')
def application(env, start_response):
    start_response('200 OK', [('Content-Type','text/html')])
    return [b"Hello World"]
