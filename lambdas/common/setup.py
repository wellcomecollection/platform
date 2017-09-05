#!/usr/bin/env python

import os

from setuptools import setup, find_packages


def local_file(name):
    return os.path.relpath(os.path.join(os.path.dirname(__file__), name))

SOURCE = local_file('src')

setup(
    name="wellcome_lambdas_common",
    version="0.2",
    packages=find_packages(SOURCE),
    author='Robert Kenny',
    author_email='R.Kenny@wellcome.ac.uk',
    url='https://github.com/wellcometrust/platform-api/lambdas/common',
    package_dir={'': SOURCE},
    python_requires='>=3.6',
    install_requires=[
        'simplejson==3.11.1',
        'six==1.10.0',
        'structlog==17.2.0',
        'boto3>=1.4.5'
    ]
)