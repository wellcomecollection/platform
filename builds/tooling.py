# -*- encoding: utf-8 -*-

import errno
import os
import shlex
import subprocess
import zipfile


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

# Hash of the current commit
CURRENT_COMMIT = subprocess.check_output([
    'git', 'rev-parse', 'HEAD']).decode('ascii').strip()

# Environment from environment environment variable!
DEFAULT_BUILD_ENV = 'dev'
PLATFORM_ENV = os.getenv('PLATFORM_ENV', DEFAULT_BUILD_ENV)


def write_release_id(project, release_id):
    """
    Write a release ID to the .releases directory in the root of the repo.
    """
    releases_dir = os.path.join(ROOT, '.releases')
    os.makedirs(releases_dir, exist_ok=True)

    release_file = os.path.join(releases_dir, project)
    with open(release_file, 'w') as f:
        f.write(release_id)


def ecr_repo_uri_from_name(ecr_client, name):
    """
    Given the name of an ECR repo (e.g. uk.ac.wellcome/api), return the URI
    for the repo.
    """
    resp = ecr_client.describe_repositories(repositoryNames=[name])
    try:
        return resp['repositories'][0]['repositoryUri']
    except (KeyError, IndexError) as e:
        raise RuntimeError('Unable to look up repo URI for %r: %s' % (name, e))


def ecr_login():
    """
    Authenticates for pushing to ECR.
    """
    command = subprocess.check_output([
        'aws', 'ecr', 'get-login', '--no-include-email'
    ]).decode('ascii')
    subprocess.check_call(shlex.split(command))


def mkdir_p(path):
    """Create a directory if it doesn't already exist."""
    # https://stackoverflow.com/a/600612/1558022
    try:
        os.makedirs(path)
    except OSError as exc:  # Python >2.5
        if exc.errno == errno.EEXIST and os.path.isdir(path):
            pass
        else:
            raise


def compare_zip_files(zf1, zf2):
    """Return True/False if ``zf1`` and ``zf2`` have the same contents.

    This ignores file metadata (e.g. creation time), and just looks at
    filenames and CRC-32 checksums.

    This requires zipcmp to be available.
    """
    try:
        subprocess.check_call(['zipcmp', '-q', zf1, zf2])
    except subprocess.CalledProcessError:
        return False
    else:
        return True
