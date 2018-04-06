# -*- encoding: utf-8

import collections
import os

Project = collections.namedtuple('Project', ['name', 'type', 'exclusive_path'])


def get_projects(repo):
    for root, _, filenames in os.walk(repo):
        if any(
            dirname in root
            for dirname in ('.terraform', 'target', 'node_modules')
        ):
            continue
        for f in filenames:
            if f == 'Makefile':
                path = os.path.join(root, f)
                for proj in _get_projects_from_makefile(root=root, path=path):
                    yield proj


def _get_projects_from_makefile(root, path):
    contents = open(path).read()

    # Newlines in Makefiles are escaped with backslashes
    contents = contents.replace('\\\n', ' ')
    lines = contents.splitlines()

    for make_variable, project_type in [
        ('SBT_APPS', 'sbt_app'),
        ('ECS_TASKS', 'ecs_task'),
        ('LAMBDAS', 'python_lambda'),
    ]:
        matching_lines = [l for l in lines if l.startswith(make_variable)]

        if len(matching_lines) == 0:
            continue
        elif len(matching_lines) == 1:
            # The line is 'SBT_APPS = foo bar baz', so we discard the first
            # two terms in the line.
            targets = matching_lines[0].split()[2:]
        else:  # pragma: no cover
            raise RuntimeError(
                "Too many %s variables in %s" % (make_variable, path)
            )

        for t in targets:
            yield Project(
                name=t,
                type=project_type,
                exclusive_path=os.path.join(root, t)
            )
