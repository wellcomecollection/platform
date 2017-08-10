data "template_file" "job_queue" {
  template = "${file("${path.module}/create_batch_queue.json.template")}"

  vars {
    name             = "${var.name}"
    compute_env_name = "${var.compute_env_name}"
  }
}

resource "null_resource" "export_rendered_template" {
  triggers {
    template = "${data.template_file.job_queue.rendered}"
  }

  provisioner "local-exec" {
    command = "cat > /app/batch_queue_${var.name}.json <<EOL\n${data.template_file.job_queue.rendered}\nEOL"

    on_failure = "fail"
  }

  provisioner "local-exec" {
    command = "aws_batch_helper queue create /app/batch_queue_${var.name}.json"

    on_failure = "fail"
  }
}
