locals {
  # Which version of the API is production? (romulus | remus)
  production_api = "romulus"

  pinned_romulus_api             = "304a9db0d4db377b953b040386a8cafa9d912d9f"
  pinned_romulus_api_nginx-delta = "3dd8a423123e1d175dd44520fcf03435a5fc92c8"

  pinned_remus_api             = ""
  pinned_remus_api_nginx-delta = ""
}
