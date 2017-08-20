#!/usr/bin/env python3
# -*- encoding: utf-8 -*-

import os

from autogen_helpers import ROOT, run_autogen


run_autogen(
    datafile='licenses.csv',
    outfile=os.path.join(
        ROOT, 'common/src/main/scala/uk/ac/wellcome', 'models', 'License.scala'
    ),
    template_name='License.scala'
)
