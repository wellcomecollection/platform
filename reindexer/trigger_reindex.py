#!/usr/bin/env python
# -*- encoding: utf-8
"""
Create/update reindex shards in the reindex shard tracker table.

This script has a bunch of presets -- you should use it most of the time.

Usage: trigger_reindex.py --reason=<REASON> (miro | miro_migration | sierra | sierra_items) (reporting | catalogue) (--partial | --complete)
       trigger_reindex.py -h | --help

"""

import sys

import docopt

from send_reindex_messages import run_reindex


def main():
    args = docopt.docopt(__doc__)

    kwargs = {"reason": args["--reason"]}

    for src in ("miro", "miro_migration", "sierra", "sierra_items"):
        if args[src]:
            source = src
            break

    for dst in ("--partial", "--complete"):
        if args[dst]:
            destination = dst.strip("-")
            break

    # The migration and items table aren't part of the main catalogue pipeline,
    # so we don't need to check if the pipeline is empty before reindexing them.
    if source in {"miro_migration", "sierra_items"}:
        skip_pipeline_checks = True
    else:
        skip_pipeline_checks = False

    kwargs["skip_pipeline_checks"] = skip_pipeline_checks

    # Construct the table name.
    table_names = {
        "miro": "vhs-miro-complete",
        "miro_migration": "vhs-miro-migration",
        "sierra": "vhs-sourcedata-sierra",
        "sierra_items": "vhs-sourcedata-sierra-items",
    }

    kwargs["table_name"] = table_names[source]

    kwargs["topic_name"] = f"reindex_jobs-{destination}_pipeline_{source}"

    if destination == "partial":
        kwargs["is_partial"] = True
        kwargs["max_records"] = 10
        kwargs["is_complete"] = False
    else:
        assert destination == "complete"
        kwargs["is_partial"] = False
        kwargs["total_segments"] = {
            "miro": 250,
            "miro_migration": 500,
            "sierra": 2500,
            "sierra_items": 1500,
        }[source]
        kwargs["is_complete"] = True

    run_reindex(**kwargs)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        sys.exit(1)
