/* -- Ingest -- */
resource "aws_dynamodb_table" "ingest" {
  name = "${local.resource_prefix}-ingest"
  hash_key = "granule_id"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "granule_id"
    type = "S"
  }
}

/* -- Avalible Tiles -- */
resource "aws_dynamodb_table" "available_tiles" {
  name = "${local.resource_prefix}-available-tiles"
  hash_key = "tile_id"
  billing_mode = "PAY_PER_REQUEST"

  attribute {
    name = "tile_id"
    type = "S"
  }
}
