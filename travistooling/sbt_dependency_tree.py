#!/usr/bin/env python
# -*- encoding: utf-8

import json
import os


class Project:
    def __init__(self, name, folder, dependencies):
        self.name = name
        self.folder = folder
        self.dependencies = dependencies

    def __repr__(self):
        return "<%s name=%r folder=%r dependencies=%r>" % (
            type(self).__name__,
            self.name,
            self.folder,
            ",".join(sorted(d.name for d in self.dependencies)),
        )

    def all_folders(self):
        folders = set([self.folder])
        for d in self.dependencies:
            folders |= d.all_folders()
        return folders


class Repository:
    def __init__(self, metadata_dir):
        self.metadata_dir = metadata_dir
        self.projects = {}

        self._get_all_projects()

    def _get_all_projects(self):
        for f in os.listdir(self.metadata_dir):
            self.get_project(name=os.path.splitext(f)[0])

    def get_project(self, name):
        if name not in self.projects:
            data = json.load(open(os.path.join(self.metadata_dir, name + ".json")))

            project = Project(
                name=data["id"],
                folder=data["folder"],
                dependencies=[self.get_project(d) for d in data["dependencyIds"]],
            )

            self.projects[name] = project

        return self.projects[name]


if __name__ == "__main__":
    repo = Repository("builds/sbt_metadata")
    while True:
        no_deps = [k for k, v in repo.projects.items() if not v.dependencies]
        for d in no_deps:
            print(d)

        for k in no_deps:
            del repo.projects[k]

        for k, v in repo.projects.items():
            for d in no_deps:
                v.dependencies = [dd for dd in v.dependencies if dd.name != d]

        if not repo.projects:
            break

        print("~~~")
        # break

    # print(repo.projects)
