#!/usr/bin/env python
# -*- encoding: utf-8 -*-

import os

from loris.webapp import create_app

application = create_app(
    debug=False,
    config_file_path=os.environ['LORIS_CONF_FILE'])
