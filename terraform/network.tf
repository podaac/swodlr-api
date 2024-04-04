data "aws_vpc" "default" {
    tags = {
        "Name": "Application VPC"
    }
}

data "aws_subnets" "private" {
    filter {
        name   = "vpc-id"
        values = [data.aws_vpc.default.id]
    }

    filter {
        name   = "tag:Name"
        values = ["Private application*"]
    }
}

data "aws_subnet" "private" {
    for_each = toset(data.aws_subnets.private.ids)
    id = each.key
    vpc_id = data.aws_vpc.default.id
}

/* -- App Mesh -- */

resource "aws_appmesh_mesh" "mesh" {
    name = "${local.resource_prefix}-mesh"
}

resource "aws_appmesh_virtual_gateway" "gateway" {
    name = "${local.resource_prefix}-gateway"
    mesh_name = aws_appmesh_mesh.mesh.name

    spec {
        listener {
            port_mapping {
                port = 80
                protocol = "http"
            }

            tls {
                certificate {
                    acm {
                        certificate_arn = aws_acm_certificate.api.arn
                    }
                }

                mode = "STRICT"
            }
        }
    }
}

resource "aws_acm_certificate" "api" {
  domain_name       = "${local.resource_prefix}-api.internal.earthdatacloud.nasa.gov"
 
  certificate_authority_arn = data.aws_ssm_parameter.private_ca.value
 
  lifecycle {
    create_before_destroy = true
  }
}
