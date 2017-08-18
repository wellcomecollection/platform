# -*- encoding: utf-8 -*-
"""
Autogen for the License type.
"""

import csv
import os

from . import ENV, ROOT, REFERENCE_DATA


LICENSE_IN = os.path.join(REFERENCE_DATA, 'licenses.csv')
LICENSE_OUT = os.path.join(
    ROOT, 'common/src/main/scala/uk/ac/wellcome', 'models', 'License.scala'
)


def run_autogen():
    print(f'*** Running autogen for {LICENSE_OUT}')
    os.makedirs(os.path.dirname(LICENSE_OUT), exist_ok=True)

    with open(LICENSE_IN) as csvfile:
        licenses = list(csv.DictReader(csvfile))

    template = ENV.get_template('License.scala')

    with open(LICENSE_OUT, 'w') as outfile:
        outfile.write(template.render(licenses=licenses) + '\n')
