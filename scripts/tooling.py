# -*- encoding: utf-8 -*-

import base64
import os
import subprocess


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

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


def authenticate_for_ecr_pushes(ecr_client, docker_client, repo_uri):
    """
    Get a login token from ECR, and authenticate ourselves to do 'docker push'.
    """
    resp = ecr_client.get_authorization_token()
    token = resp['authorizationData'][0]['authorizationToken']
    username, password = base64.b64decode(token).decode().split(':')

    resp = docker_client.login(
        username=username,
        password=password,
        registry=repo_uri
    )
    print(resp)


def write_release_id(project, release_id):
    """
    Write a release ID to the .releases directory in the root of the repo.
    """
    releases_dir = os.path.join(ROOT, '.releases')
    os.makedirs(releases_dir, exist_ok=True)

    release_file = os.path.join(releases_dir, project)
    with open(release_file, 'w') as f:
        f.write(release_id)
