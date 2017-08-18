# -*- encoding: utf-8

import os
import subprocess

from jinja2 import Environment, PackageLoader


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel'
]).decode('ascii').strip()

# Directory containing reference data for autogen
REFERENCE_DATA = os.path.join(ROOT, 'ontologies', 'Reference data')

AUTOGEN_OUT = os.path.join(
    ROOT, 'common', 'src', 'main', 'scala', 'uk', 'ac', 'wellcome', 'autogen'
)

ENV = Environment(loader=PackageLoader('autogen_helpers', 'templates'))
