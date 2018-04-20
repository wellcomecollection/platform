# -*- encoding: utf-8
"""
This test doesn't test the travistooling library directly, but it makes
certain assertions about our .travis.yml file.
"""

import os
import yaml

from travistooling import ROOT
from travistooling.parse_makefiles import PROJECTS


TRAVIS_YML = yaml.safe_load(open(os.path.join(ROOT, '.travis.yml')))


def test_travis_yml_includes_scala_target_dirs():
    """
    Each Scala app has a target directory, e.g. reindexer/reindex_worker/target

    We cache these directories to save on the cost of a clean rebuild on
    every Travis run.  Check we're caching exactly these directories, and
    only these directories.
    """
    actual_target_dirs = [
        d
        for d in TRAVIS_YML['cache']['directories']
        if d.endswith('/target')
    ]

    # There's currently no way for travistooling to infer the existence of
    # these common lib projects, so we hard-code them here for now.
    expected_target_dirs = [
        'common/target',
        'sbt_common/display/target',
        'sbt_common/elasticsearch/target',
        'sierra_adapter/common/target',
    ]

    for p in PROJECTS:
        if p.type == 'sbt_app':
            project_dir = os.path.relpath(p.exclusive_path, start=ROOT)
            target_dir = os.path.join(project_dir, 'target')
            expected_target_dirs.append(target_dir)

    assert sorted(expected_target_dirs) == sorted(actual_target_dirs)
