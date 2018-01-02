# sierra_window_generator

This module creates the Lambda that generates update windows for the sierra_to_dynamo apps, e.g.

```json
{
    "start": "2017-11-08T00:01:01+00:00",
    "end": "2017-11-31T23:59:59+00:00"
}
```

It exports the name of the queue of created windows.
