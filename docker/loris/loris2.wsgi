#!/usr/bin/env python
# -*- encoding: utf-8 -*-

from werkzeug.contrib.profiler import ProfilerMiddleware

from loris.webapp import create_app

application = ProfilerMiddleware(create_app(
    debug=False,
    config_file_path='/opt/loris/etc/loris2.conf'))
