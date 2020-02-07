# Admin role

module "admin_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-admin"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "admin_role_policy" {
  source    = "../../role_policies/admin"
  role_name = module.admin_role.name
}

# Billing role

module "billing_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-billing"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "billing_role_policy" {
  source    = "../../role_policies/billing"
  role_name = module.billing_role.name
}

# Developer role

module "developer_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-developer"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "developer_role_policy" {
  source    = "../../role_policies/developer"
  role_name = module.developer_role.name
}

# Monitoring role

module "monitoring_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-monitoring"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "monitoring_role_policy" {
  source    = "../../role_policies/monitoring"
  role_name = module.monitoring_role.name
}

# Read/only role

module "read_only_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-read_only"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "read_only_role_policy" {
  source    = "../../role_policies/read_only"
  role_name = module.read_only_role.name
}

# Publisher role

module "publisher_role" {
  source = "../../assumable_role/aws"
  name   = "${var.prefix}-publisher"

  max_session_duration_in_seconds = var.max_session_duration_in_seconds

  principals = var.principals
}

module "publisher_role_policy" {
  source    = "../../role_policies/publisher"
  role_name = module.publisher_role.name
}
