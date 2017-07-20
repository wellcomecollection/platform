# cache_cleaner

This service allows us to purge our EFS cache based on the age and size of items.

Some of our ECS applications use local storage as a cache (for example, Loris).
Because ECS instances have very little disk space, we use an EFS mount rather than the host disk.
However, EFS storage has a price – although it can grow to any size, it would be expensive to let it do so!
This service deletes files from our EFS cache to keep our costs sustainable.

Our caches are bounded by two parameters:

*   **maximum age** -- if an item is in the cache but hasn't been accessed for more than 30 days, there isn't much value in keeping it in the cache.
    We can delete it, and re-fetch it the next time it's requested.

*   **maximum size** -- EFS storage is priced based on how much you use.
    If the cache exceeds a given size, we can delete items from the cache until it comes back under the limit.
    Items are deleted in order of last access time -- items which haven't been accessed very recently are deleted first.

Both of these parameters are configurable.

## Usage

Build the Docker image containing the script:

```console
$ docker build -t efs_cache_cleaner .
```

To run the script, share the cache directory with the container and pass parameters as environment variables:

```console
$ docker run -v /path/to/cache:/data -e MAX_AGE=30 -e MAX_SIZE=5000000 efs_cache_cleaner
```
