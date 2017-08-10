# elasticsearch_cleaner

This service deletes unused indices from our Elasticsearch cluster.

As we update our data model, we're indexing our data into new Elasticsearch indices (whether using the Elastic APIs or our reindexer service).
Deleting the old indices frees up space on the cluster.
