# This file creates a JSON string that gets passed as config to the reindexer.
#
# The structure of the JSON object is something like this:
#
#     {
#       "id1": {
#         "dynamoConfig": {"table": "mytable1"},
#         "snsConfig": {"topicArn": "mytopic1"}
#       },
#       "id2": {
#         "dynamoConfig": {"table": "mytable2"},
#         "snsConfig": {"topicArn": "mytopic2"}
#       }
#     }
#
# It corresponds to a Map[String, ReindexJobConfig] in the Scala classes within
# the reindexer, and tells it what combinations of table -> topic are allowed.
#
# Naturally, Terraform would not be as helpful as to provide us some loop
# constructs or anything useful, so we have to construct the string by hand.
# This file does that.

# This template contains the JSON string for a *single* entry in the job config.

data "template_file" "single_reindex_job_config" {
  template = <<EOF
  "$${id}": {
    "dynamoConfig": {
      "table": "$${table}"
    },
    "snsConfig": {
      "topicArn": "$${topicArn}"
    }
  }
EOF

  count = "${length(local.reindexer_jobs)}"

  vars {
    id       = "${lookup(local.reindexer_jobs[count.index], "id")}"
    table    = "${lookup(local.reindexer_jobs[count.index], "table")}"
    topicArn = "${lookup(local.reindexer_jobs[count.index], "topic")}"
  }
}

# This template takes all of those JSON strings, and combines them into a single
# JSON object.

data "template_file" "all_reindex_job_config" {
  template = <<EOF
  {
    $${value}
  }
EOF

  vars {
    value = "${join(",", data.template_file.single_reindex_job_config.*.rendered)}"
  }
}

# For an idea of what's going on, uncomment this line and run a 'terraform apply':
# output "all_reindex_job_config" { value = "${data.template_file.all_reindex_job_config.rendered }"}

# This renders the template, and removes all the whitespace so it can be passed
# as a single line.
locals {
  reindex_job_config_json = "${replace(data.template_file.all_reindex_job_config.rendered, "/\\s/", "")}"
}

output "reindex_job_config_json" {
  value = "${local.reindex_job_config_json}"
}
