# -*- encoding: utf-8 -*-

from jinja2 import Environment, PackageLoader

from tooling import ROOT, REFERENCE_DATA

env = Environment(loader=PackageLoader('autogen', 'templates'))
