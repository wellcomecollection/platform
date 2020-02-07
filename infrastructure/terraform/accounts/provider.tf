provider "aws" {
  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::760097843905:role/platform-admin"
  }
}

provider "aws" {
  alias = "catalogue"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::756629837203:role/catalogue-admin"
  }
}

provider "aws" {
  alias = "workflow"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::299497370133:role/workflow-admin"
  }
}

provider "aws" {
  alias = "storage"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::975596993436:role/storage-admin"
  }
}

provider "aws" {
  alias = "digitisation"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::404315009621:role/digitisation-admin"
  }
}

provider "aws" {
  alias = "data"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::964279923020:role/data-admin"
  }
}

provider "aws" {
  alias = "reporting"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::269807742353:role/reporting-admin"
  }
}

provider "aws" {
  alias = "experience"

  region  = "eu-west-1"
  version = "2.35.0"

  assume_role {
    role_arn = "arn:aws:iam::130871440101:role/experience-admin"
  }
}