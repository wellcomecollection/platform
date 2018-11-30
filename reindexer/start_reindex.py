#!/usr/bin/env python
# -*- encoding: utf-8

import click


SOURCES = [
    "miro",
    "sierra",
    "sierra_items"
]

DESTINATIONS = [
    "catalogue",
    "reporting"
]


@click.command()
@click.option(
    "--src", type=click.Choice(SOURCES), required=True,
    prompt="Which source do you want to reindex? (%s)" % ", ".join(SOURCES),
    help="Name of the source to reindex"
)
@click.option(
    "--dst", type=click.Choice(DESTINATIONS), required=True,
    prompt="Which pipeline are you sending this to? (%s)" % ", ".join(DESTINATIONS),
    help="Name of the pipeline to receive the reindexed records"
)
@click.option(
    "--mode", type=click.Choice(["complete", "partial"]), required=True,
    prompt="Every record (complete) or just a few (partial)?",
    help="Should this reindex send every record (complete) or just a few (partial)?"
)
@click.option(
    "--reason", prompt="Why are you running this reindex?",
    help="The reason to run this reindex"
)
def start_reindex(src, dst, mode, reason):
    print(src)
    print(dst)
    print(mode)
    print(reason)


if __name__ == '__main__':
    start_reindex()
