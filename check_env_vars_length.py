#!/usr/bin/env python
# -*- encoding: utf-8

import logging
import os
import sys

import daiquiri
import hcl


daiquiri.setup(level=logging.INFO)
logger = daiquiri.getLogger(__name__)


def main():
    failures = set()

    for root, _, filenames in os.walk('.'):
        for f in filenames:
            if not f.endswith('.tf'):
                continue
            path = os.path.join(root, f)

            tf_content = open(path).read()
            if 'env_vars' not in tf_content:
                continue

            terraform = hcl.loads(tf_content)

            logger.info('Checking Terraform for %s...', path)
            modules = terraform['module']

            for name, data in modules.items():
                try:
                    env_vars = data['env_vars']
                    env_vars_length = data['env_vars_length']
                except KeyError:
                    continue
                else:
                    if len(env_vars) != env_vars_length:
                        failures.add(path)
                        logger.error(
                            f"Module %s has mismatched env_vars/length: %d != %d",
                            name, len(env_vars), env_vars_length
                        )


    if failures:
        print()
        logger.error("Errors in the following files:")
        for f in failures:
            logger.error("- %s", f)
        return 1
    else:
        return 0


if __name__ == '__main__':
    sys.exit(main())
