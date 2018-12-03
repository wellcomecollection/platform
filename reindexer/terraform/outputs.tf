# This output is used by the `start_reindex.py` script to determine which
# topic it should send requests to.
#
# Don't change it without changing the corresponding script code.
#
output "topic_arn" {
  value = "${module.reindex_worker.topic_arn}"
}
