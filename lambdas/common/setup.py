#!/usr/bin/env python

import os

from setuptools import setup, find_packages

setup(
    name="common",
    version="0.2",
    packages=find_packages(exclude=['test*']),
    author='Robert Kenny',
    author_email='R.Kenny@wellcome.ac.uk',
    url='https://github.com/wellcometrust/platform-api/lambdas/common',
    python_requires='>=3.6',
    install_requires=[
        'simplejson==3.11.1',
        'six==1.10.0',
        'structlog==17.2.0',
        'boto3>=1.4.5'
    ]
)