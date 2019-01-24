# -*- encoding: utf-8

import os

import pytest

from travistooling.git_utils import ROOT
from travistooling.sbt_dependency_tree import Project, Repository


@pytest.fixture()
def repo():
    return Repository(metadata_dir=os.path.join(ROOT, "builds", "sbt_metadata"))


def test_all_folders(repo):
    p = repo.get_project("common")
    assert p.all_folders() == {"sbt_common/common"}


def test_nested_all_folders(repo):
    p = repo.get_project("config_core")
    assert p.all_folders() == {"sbt_common/common", "sbt_common/config/core"}


def test_project_repr(repo):
    p = repo.get_project("common")
    print(p)
