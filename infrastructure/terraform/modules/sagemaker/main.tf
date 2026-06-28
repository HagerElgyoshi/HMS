# =============================================================================
#  SageMaker Module — HakimAI Arabic Medical Chatbot
#  Deploys a Real-time Inference Endpoint on ml.g4dn.xlarge (GPU)
# =============================================================================

# ── IAM Role for SageMaker ────────────────────────────────────────────────────
resource "aws_iam_role" "sagemaker_execution" {
  name = "${var.project_name}-${var.environment}-sagemaker-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "sagemaker.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = var.tags
}

resource "aws_iam_role_policy_attachment" "sagemaker_full" {
  role       = aws_iam_role.sagemaker_execution.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonSageMakerFullAccess"
}

resource "aws_iam_role_policy_attachment" "ecr_read" {
  role       = aws_iam_role.sagemaker_execution.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly"
}

resource "aws_iam_role_policy_attachment" "s3_read" {
  role       = aws_iam_role.sagemaker_execution.name
  policy_arn = "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess"
}

# ── SageMaker Model ──────────────────────────────────────────────────────────
resource "aws_sagemaker_model" "chatbot" {
  name               = "${var.project_name}-${var.environment}-chatbot"
  execution_role_arn = aws_iam_role.sagemaker_execution.arn

  primary_container {
    image          = var.chatbot_image_uri
    mode           = "SingleModel"
    environment = {
      QDRANT_PATH = "/app/medical_qdrant_db"
    }
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-chatbot-model"
  })
}

# ── Endpoint Configuration ───────────────────────────────────────────────────
resource "aws_sagemaker_endpoint_configuration" "chatbot" {
  name = "${var.project_name}-${var.environment}-chatbot-config"

  production_variants {
    variant_name           = "primary"
    model_name             = aws_sagemaker_model.chatbot.name
    instance_type          = var.instance_type
    initial_instance_count = var.instance_count

    # Container startup can take 5-10 min (model download)
    container_startup_health_check_timeout_in_seconds = 600
    model_data_download_timeout_in_seconds            = 600
  }

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-chatbot-endpoint-config"
  })
}

# ── SageMaker Endpoint ───────────────────────────────────────────────────────
resource "aws_sagemaker_endpoint" "chatbot" {
  name                 = "${var.project_name}-${var.environment}-chatbot"
  endpoint_config_name = aws_sagemaker_endpoint_configuration.chatbot.name

  tags = merge(var.tags, {
    Name = "${var.project_name}-${var.environment}-chatbot-endpoint"
  })
}

# ── Auto-scaling (scale to 0 when idle for cost savings) ─────────────────────
resource "aws_appautoscaling_target" "chatbot" {
  count              = var.enable_autoscaling ? 1 : 0
  max_capacity       = var.max_instance_count
  min_capacity       = var.min_instance_count
  resource_id        = "endpoint/${aws_sagemaker_endpoint.chatbot.name}/variant/primary"
  scalable_dimension = "sagemaker:variant:DesiredInstanceCount"
  service_namespace  = "sagemaker"
}

resource "aws_appautoscaling_policy" "chatbot_scale" {
  count              = var.enable_autoscaling ? 1 : 0
  name               = "${var.project_name}-chatbot-scaling"
  policy_type        = "TargetTrackingScaling"
  resource_id        = aws_appautoscaling_target.chatbot[0].resource_id
  scalable_dimension = aws_appautoscaling_target.chatbot[0].scalable_dimension
  service_namespace  = aws_appautoscaling_target.chatbot[0].service_namespace

  target_tracking_scaling_policy_configuration {
    predefined_metric_specification {
      predefined_metric_type = "SageMakerVariantInvocationsPerInstance"
    }
    target_value       = 5.0
    scale_in_cooldown  = 300
    scale_out_cooldown = 60
  }
}
