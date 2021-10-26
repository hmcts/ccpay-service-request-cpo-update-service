provider "azurerm" {
  features {}
}

locals {
  subscription_name = "serviceRequestCpoUpdateSubscription"
}

data "azurerm_resource_group" "rg" {
  name     = join("-", [var.product, var.env])
}

data "azurerm_key_vault" "ccpay_key_vault" {
  name = join("-", [var.product, var.env])
  resource_group_name = join("-", [var.product, var.env])
}

data "azurerm_servicebus_namespace" "ccpay_servicebus_namespace" {
  name                = join("-", [var.product, "servicebus", var.env])
  resource_group_name = join("-", [var.product, var.env])
}

module "service_request_cpo_update_topic" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-topic"
  name                  = "ccpay-service-request-cpo-update-topic"
  namespace_name        = data.azurerm_servicebus_namespace.ccpay_servicebus_namespace.name
  resource_group_name   = data.azurerm_resource_group.rg.name
}

module "service_request_cpo_update_subscription" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-subscription"
  name                  = local.subscription_name
  namespace_name        = data.azurerm_servicebus_namespace.ccpay_servicebus_namespace.name
  resource_group_name   = data.azurerm_resource_group.rg.name
  topic_name            = module.service_request_cpo_update_topic
}

resource "azurerm_key_vault_secret" "ccpay_service_request_cpo_update_topic_shared_access_key" {
  name         = "ccpay-service-request-cpo-update-topic-shared-access-key"
  value        = module.service_request_cpo_update_topic.primary_send_and_listen_shared_access_key
  key_vault_id = data.azurerm_key_vault.ccpay_key_vault.id
}

