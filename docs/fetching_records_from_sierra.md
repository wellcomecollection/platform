# Fetching records from Sierra

In general, the Sierra adapter stack will keep us up to date with new records
from Sierra, and it shouldn't need any manual intervention.

If you do need to fill in some gaps manually, this document explains how to
do it.

## What have we ingested?

If you want to see how many records we've already pulled in, there's a script
that reports the current adapter progress:

```console
$ python sierra_adapter/report_adapter_progress.py

===============================================================================
bibs windows
===============================================================================
2003-05-01T00:00:00 -- 2018-03-07T16:31:21.573345


===============================================================================
items windows
===============================================================================
1999-11-01T00:01:00 -- 2018-03-07T16:28:51.955944
```

These represent a complete run, through to the very earliest bibs and items
we need to pull in.

## Filling in some gaps

If the script above shows gaps in the data, you can regenerate the windows
for the Sierra reader with a second script.

For example:

```console
$ python sierra_adapter/build_windows.py \
  --interval=5 \
  --resource=items \
  --start='2018-02-02T16:27' \
  --end='2018-02-02T16:44'
```

Here "interval" is measured in minutes.  Keep running this script to create
new windows until you're done!
