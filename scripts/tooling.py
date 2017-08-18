# -*- encoding: utf-8 -*-

import os
import shlex
import subprocess


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

# Directory containing reference data for autogen
REFERENCE_DATA = os.path.join(ROOT, 'ontologies', 'Reference data')

AUTOGEN_OUT = os.path.join(
    ROOT, 'common', 'src', 'main', 'scala', 'uk', 'ac', 'wellcome', 'autogen'
)

# Hash of the current commit
CURRENT_COMMIT = subprocess.check_output([
    'git', 'rev-parse', 'HEAD']).decode('ascii').strip()

# Environment from environment environment variable!
DEFAULT_BUILD_ENV = 'dev'
PLATFORM_ENV = os.getenv('PLATFORM_ENV', DEFAULT_BUILD_ENV)


def changed_files(commit_range):
    """
    Returns a set of changed files in a given commit range.

    :param commit_range: A list of arguments to pass to ``git diff``.

    """
    files = set()
    command = ['git', 'diff', '--name-only'] + commit_range
    diff_output = subprocess.check_output(command).decode('ascii')
    for line in diff_output.splitlines():
        filepath = line.strip()
        if filepath:
            files.add(filepath)
    return files


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


def write_release_id(project, release_id):
    """
    Write a release ID to the .releases directory in the root of the repo.
    """
    releases_dir = os.path.join(ROOT, '.releases')
    os.makedirs(releases_dir, exist_ok=True)

    release_file = os.path.join(releases_dir, project)
    with open(release_file, 'w') as f:
        f.write(release_id)
