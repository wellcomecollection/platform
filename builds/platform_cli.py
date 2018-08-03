#!/usr/bin/env python
# -*- encoding: utf-8
"""
This script contains all our release tooling for sbt libraries.

Usage:
    platform_cli.py autoformat
    platform_cli.py check_release_file
    platform_cli.py release
    platform_cli.py test
    platform_cli.py -h | --help

Commands:
    check_release_file      Runs in a pull request to check if the RELEASE.md
                            file is well-formatted.  Exits with 0 if correct,
                            exits with 1 if not.
    release                 Publish a new release of the library.
    test                    Run Scala tests.

The canonical version of this script is kept in the platform repo
(https://github.com/wellcometrust/platform), but copied into our other
repos for the sake of easy distribution.

Implementor note: any repo-specific config should be kept *outside* this
script.  Put it in .travis.yml or build.sbt.

"""

import datetime as dt
import os
import re
import shutil
import subprocess
import sys


ROOT = subprocess.check_output([
    'git', 'rev-parse', '--show-toplevel']).decode('ascii').strip()

BUILD_SBT = os.path.join(ROOT, 'build.sbt')


def git(*args):
    """
    Run a Git command and check it completes successfully.
    """
    subprocess.check_call(('git',) + args)


def sbt(*args):
    """
    Run an sbt command and check it completes successfully.
    """
    subprocess.check_call(('sbt',) + args)


def tags():
    """
    Returns a list of all tags in the repo.
    """
    git('fetch', '--tags')
    result = subprocess.check_output(['git', 'tag']).decode('ascii').strip()
    all_tags = result.split('\n')

    assert len(set(all_tags)) == len(all_tags)

    return set(all_tags)


def latest_version():
    """
    Returns the latest version, as specified by the Git tags.
    """
    versions = []

    for t in tags():
        assert t == t.strip()
        parts = t.split('.')
        assert len(parts) == 3, t
        parts[0] = parts[0].lstrip('v')
        v = tuple(map(int, parts))

        versions.append((v, t))

    _, latest = max(versions)

    assert latest in tags()
    return latest


def modified_files():
    """
    Returns a list of all files which have been modified between now
    and the latest release.
    """
    files = set()
    for command in [
        ['git', 'diff', '--name-only', '--diff-filter=d',
            latest_version(), 'HEAD'],
        ['git', 'diff', '--name-only']
    ]:
        diff_output = subprocess.check_output(command).decode('ascii')
        for l in diff_output.split('\n'):
            filepath = l.strip()
            if filepath:
                assert os.path.exists(filepath)
                files.add(filepath)
    return files


def has_source_changes():
    """
    Returns True if there are source changes since the previous release,
    False if not.
    """
    changed_files = [
        f for f in modified_files() if f.strip().endswith(('.sbt', '.scala'))
    ]
    return len(changed_files) != 0


RELEASE_FILE = os.path.join(ROOT, 'RELEASE.md')


def has_release():
    """
    Returns True if there is a release file, False if not.
    """
    return os.path.exists(RELEASE_FILE)


RELEASE_TYPE = re.compile(r"^RELEASE_TYPE: +(major|minor|patch)")

MAJOR = 'major'
MINOR = 'minor'
PATCH = 'patch'

VALID_RELEASE_TYPES = (MAJOR, MINOR, PATCH)


def parse_release_file():
    """
    Parses the release file, returning a tuple (release_type, release_contents)
    """
    with open(RELEASE_FILE) as i:
        release_contents = i.read()

    release_lines = release_contents.split('\n')

    m = RELEASE_TYPE.match(release_lines[0])
    if m is not None:
        release_type = m.group(1)
        if release_type not in VALID_RELEASE_TYPES:
            print('Unrecognised release type %r' % (release_type,))
            sys.exit(1)
        del release_lines[0]
        release_contents = '\n'.join(release_lines).strip()
    else:
        print(
            'RELEASE.md does not start by specifying release type. The first '
            'line of the file should be RELEASE_TYPE: followed by one of '
            'major, minor, or patch, to specify the type of release that '
            'this is (i.e. which version number to increment). Instead the '
            'first line was %r' % (release_lines[0],)
        )
        sys.exit(1)

    return release_type, release_contents


def check_release_file():
    if has_source_changes():
        if not has_release():
            print(
                'There are source changes but no RELEASE.md. Please create '
                'one to describe your changes.'
            )
            sys.exit(1)
        parse_release_file()


def hash_for_name(name):
    return subprocess.check_output([
        'git', 'rev-parse', name
    ]).decode('ascii').strip()


def is_ancestor(a, b):
    check = subprocess.call([
        'git', 'merge-base', '--is-ancestor', a, b
    ])
    assert 0 <= check <= 1
    return check == 0


CHANGELOG_HEADER = re.compile(r"^## v\d+\.\d+\.\d+ - \d\d\d\d-\d\d-\d\d$")
CHANGELOG_FILE = os.path.join(ROOT, 'CHANGELOG.md')


def changelog():
    with open(CHANGELOG_FILE) as i:
        return i.read()


def new_version(release_type):
    version = latest_version()
    version_info = [int(i) for i in version.lstrip('v').split('.')]

    new_version = list(version_info)
    bump = VALID_RELEASE_TYPES.index(release_type)
    new_version[bump] += 1
    for i in range(bump + 1, len(new_version)):
        new_version[i] = 0
    new_version = tuple(new_version)
    return 'v' + '.'.join(map(str, new_version))


def update_changelog_and_version():
    contents = changelog()
    assert '\r' not in contents
    lines = contents.split('\n')
    assert contents == '\n'.join(lines)
    for i, l in enumerate(lines):
        if CHANGELOG_HEADER.match(l):
            beginning = '\n'.join(lines[:i])
            rest = '\n'.join(lines[i:])
            assert '\n'.join((beginning, rest)) == contents
            break

    release_type, release_contents = parse_release_file()

    new_version_string = new_version(release_type)

    print('New version: %s' % new_version_string)

    now = dt.datetime.utcnow()

    date = max([
        d.strftime('%Y-%m-%d') for d in (now, now + dt.timedelta(hours=1))
    ])

    heading_for_new_version = '## ' + ' - '.join((new_version_string, date))

    new_changelog_parts = [
        beginning.strip(),
        '',
        heading_for_new_version,
        '',
        release_contents,
        '',
        rest
    ]

    with open(CHANGELOG_FILE, 'w') as o:
        o.write('\n'.join(new_changelog_parts))

    # Update the version specified in build.sbt.  We're looking to replace
    # a line of the form:
    #
    #       version := "x.y.z"
    #
    lines = list(open(BUILD_SBT))
    for idx, l in enumerate(lines):
        if l.startswith('version := '):
            lines[idx] = 'version := "%s"\n' % new_version_string.strip('v')
            break
    else:  # no break
        raise RuntimeError('Never updated version in build.sbt?')

    with open(BUILD_SBT, 'w') as f:
        f.write(''.join(lines))

    return release_type


def update_for_pending_release():
    release_type = update_changelog_and_version()

    git('rm', RELEASE_FILE)
    git('add', CHANGELOG_FILE)
    git('add', BUILD_SBT)

    git(
        'commit',
        '-m', 'Bump version to %s and update changelog\n\n[skip ci]' % (
            new_version(release_type))
    )
    git('tag', new_version(release_type))


def configure_secrets():
    subprocess.check_call(['unzip', 'secrets.zip'])

    os.makedirs(os.path.join(os.environ['HOME'], '.aws'))
    shutil.copyfile(
        src='awscredentials',
        dst=os.path.join(os.environ['HOME'], '.aws', 'credentials')
    )

    subprocess.check_call(['chmod', '600', 'id_rsa'])
    git('config', 'core.sshCommand', 'ssh -i id_rsa')

    git('config', 'user.name', 'Travis CI on behalf of Wellcome')
    git('config', 'user.email', 'wellcomedigitalplatform@wellcome.ac.uk')

    print('SSH public key:')
    subprocess.check_call(['ssh-keygen', '-y', '-f', 'id_rsa'])


def release():
    last_release = latest_version()

    print('Latest released version: %s' % last_release)

    HEAD = hash_for_name('HEAD')
    MASTER = hash_for_name('origin/master')

    print('Current head:   %s' % HEAD)
    print('Current master: %s' % MASTER)

    on_master = is_ancestor(HEAD, MASTER)

    if not on_master:
        print('Trying to release while not on master?')
        sys.exit(1)

    if has_release():
        print('Updating changelog and version')
        update_for_pending_release()
    else:
        print('Not releasing due to no release file')
        sys.exit(0)

    print('Attempting a release.')
    sbt('publish')

    git('push', 'ssh-origin', 'HEAD:master')
    git('push', 'ssh-origin', '--tag')


def branch_name():
    """Return the name of the branch under test."""
    # See https://graysonkoonce.com/getting-the-current-branch-name-during-a-pull-request-in-travis-ci/
    if os.environ['TRAVIS_PULL_REQUEST'] == 'false':
        return os.environ['TRAVIS_BRANCH']
    else:
        return os.environ['TRAVIS_PULL_REQUEST_BRANCH']


def autoformat():
    sbt('scalafmt')

    # If there are any changes, push to GitHub immediately and fail the
    # build.  This will abort the remaining jobs, and trigger a new build
    # with the reformatted code.
    if subprocess.call(['git', 'diff', '--exit-code']):
        print('There were changes from formatting, creating a commit')

        # We checkout the branch before we add the commit, so we don't
        # include the merge commit that Travis makes.
        git('fetch', 'ssh-origin')
        git('checkout', branch_name())

        git('add', '--verbose', '--update')
        git('commit', '-m', 'Apply auto-formatting rules')
        git('push', 'ssh-origin', 'HEAD:%s' % branch_name())

        # We exit here to fail the build, so Travis will skip to the next
        # build, which includes the autoformat commit.
        sys.exit(1)
    else:
        print('There were no changes from auto-formatting')


if __name__ == '__main__':

    # Rudimentary command-line argument parsing.
    #
    # It would be nice to replace this with something more robust using
    # argparse or docopt, but installing extra packages in Travis when
    # releasing a Scala library is more hassle than it's worth.
    #
    if (
        len(sys.argv) != 2 or
        sys.argv[1] in ('-h', '--help') or
        sys.argv[1] not in (
            'autoformat', 'check_release_file', 'release', 'test')
    ):
        print(__doc__.strip())
        sys.exit(1)

    configure_secrets()

    if sys.argv[1] == 'check_release_file':
        check_release_file()
    elif sys.argv[1] == 'release':
        release()
    elif sys.argv[1] == 'test':
        if os.path.exists('docker-compose.yml'):
            sbt('dockerComposeUp')
            sbt('test')
        else:
            sbt('test')
    elif sys.argv[1] == 'autoformat':
        autoformat()
    else:
        assert False, sys.argv
