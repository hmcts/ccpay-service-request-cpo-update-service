terraform {
  backend "azurerm" {}

  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "~> 4.11.0"
      api_version = "2024-11-01"
    }
  }
}
