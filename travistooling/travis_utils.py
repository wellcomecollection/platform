# -*- encoding: utf-8

import os
import shutil
import subprocess
import sys
import zipfile

from travistooling.shell_utils import check_call


def branch_name():
    """Return the name of the branch under test."""
    # See https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
    if os.environ['TRAVIS_PULL_REQUEST'] == 'false':
        return os.environ['TRAVIS_BRANCH']
    else:
        return os.environ['TRAVIS_PULL_REQUEST_BRANCH']


def unpack_secrets():  # pragma: no cover
    """
    We store our AWS credentials and SSH keys for Travis in an
    encrypted ZIP bundle.

    This unencrypts the credentials, and copies them into place.
    """
    print('*** Loading secrets for Travis')

    # Unencrypted the encrypted ZIP file.
    try:
        # We deliberately use subprocess so we can catch the error here,
        # and suppress the encrypted environment variables from appearing
        # in any exception output.
        subprocess.check_call([
            'openssl', 'aes-256-cbc',
            '-K', os.environ['encrypted_83630750896a_key'],
            '-iv', os.environ['encrypted_83630750896a_iv'],
            '-in', 'secrets.zip.enc',
            '-out', 'secrets.zip', '-d'
        ])
    except subprocess.CalledProcessError:
        print('*** Unexpected error unpacking secrets')
        sys.exit(1)
    except KeyError as err:
        print('*** Error unpacking secrets -- do you have the env vars?')
        print(err)
        sys.exit(2)

    zf = zipfile.ZipFile('secrets.zip')
    zf.extractall(path='.')

    os.makedirs(os.path.join(os.environ['HOME'], '.aws'), exist_ok=True)
    for f in ['config', 'credentials']:
        shutil.move(
            src=os.path.join('secrets', f),
            dst=os.path.join(os.environ['HOME'], '.aws', f)
        )

    check_call(['chmod', '400', 'secrets/id_rsa'])
