# -*- encoding: utf-8 -*-

import subprocess


# Root of the Git repository
ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()


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
        raise RuntimeError(f'Unable to look up repo URI for {name!r}: {e}')


def authenticate_for_ecr_pushes(ecr_client, docker_client, repo_uri):
    """
    Get a login token from ECR, and authenticate ourselves to do 'docker push'.
    """
    resp = ecr_client.get_authorization_token()
    token = resp['authorizationData'][0]['authorizationToken']

    docker_client.login(username='AWS', password='token', registry=repo_uri)
