module "vhs_recorder" {
  source = "../vhs"
  name   = "${replace(var.namespace, "_", "-")}-Recorder"
}
