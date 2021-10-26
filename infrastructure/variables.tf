variable "product" {
  type    = string
  default = "ccpay"
}

variable "component" {}

variable "location" {
  type    = string
  default = "UK South"
}

variable "env" {}

variable "subscription" {}

variable "deployment_namespace" {}

variable "common_tags" {
  type = map(string)
}
