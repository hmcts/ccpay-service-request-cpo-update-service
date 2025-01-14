provider "azurerm" {
  features {}
}

locals {
  subscription_name = "serviceRequestCpoUpdateSubscription"
  premium_subscription_name = "serviceRequestCpoUpdatePremiumSubscription"
  s2s_rg_prefix               = "rpe-service-auth-provider"
  s2s_key_vault_name          = var.env == "preview" || var.env == "spreview" ? join("-", ["s2s", "aat"]) : join("-", ["s2s", var.env])
  s2s_vault_resource_group    = var.env == "preview" || var.env == "spreview" ? join("-", [local.s2s_rg_prefix, "aat"]) : join("-", [local.s2s_rg_prefix, var.env])
}

data "azurerm_resource_group" "rg" {
  name     = join("-", [var.product, var.env])
}

data "azurerm_key_vault" "ccpay_key_vault" {
  name = join("-", [var.product, var.env])
  resource_group_name = join("-", [var.product, var.env])
}

data "azurerm_servicebus_namespace" "ccpay_premium_servicebus_namespace" {
  name                = join("-", [var.product, "servicebus", var.env, "premium"])
  resource_group_name = join("-", [var.product, var.env])
}

module "service_request_cpo_update_topic_premium" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-topic?ref=4.x"
  name                  = "ccpay-service-request-cpo-update-topic"
  namespace_name        = data.azurerm_servicebus_namespace.ccpay_premium_servicebus_namespace.name
  resource_group_name   = data.azurerm_resource_group.rg.name
}

module "service_request_cpo_update_subscription_premium" {
  source                = "git@github.com:hmcts/terraform-module-servicebus-subscription?ref=4.x"
  name                  = local.premium_subscription_name
  namespace_name        = data.azurerm_servicebus_namespace.ccpay_premium_servicebus_namespace.name
  resource_group_name   = data.azurerm_resource_group.rg.name
  topic_name            = module.service_request_cpo_update_topic_premium.name
  depends_on            = [module.service_request_cpo_update_topic_premium]
}

resource "azurerm_key_vault_secret" "ccpay_service_request_cpo_update_topic_premium_shared_access_key" {
  name         = "ccpay-service-request-cpo-update-topic-premium-shared-access-key"
  value        = module.service_request_cpo_update_topic_premium.primary_send_and_listen_shared_access_key
  key_vault_id = data.azurerm_key_vault.ccpay_key_vault.id
}

data "azurerm_key_vault" "s2s_key_vault" {
  name                = local.s2s_key_vault_name
  resource_group_name = local.s2s_vault_resource_group
}

data "azurerm_key_vault_secret" "s2s_secret" {
  name          = "microservicekey-service-request-cpo-update-service"
  key_vault_id  = data.azurerm_key_vault.s2s_key_vault.id
}

resource "azurerm_key_vault_secret" "service_request_cpo_update_service_s2s_secret" {
  name          = "service-request-cpo-update-service-s2s-secret"
  value         = data.azurerm_key_vault_secret.s2s_secret.value
  key_vault_id  = data.azurerm_key_vault.ccpay_key_vault.id
}
