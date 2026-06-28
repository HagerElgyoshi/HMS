output "endpoint_name" {
  description = "SageMaker endpoint name"
  value       = aws_sagemaker_endpoint.chatbot.name
}

output "endpoint_arn" {
  description = "SageMaker endpoint ARN"
  value       = aws_sagemaker_endpoint.chatbot.arn
}

output "endpoint_url" {
  description = "URL to invoke the endpoint (via AWS SDK)"
  value       = "https://runtime.sagemaker.${data.aws_region.current.name}.amazonaws.com/endpoints/${aws_sagemaker_endpoint.chatbot.name}/invocations"
}

output "model_name" {
  description = "SageMaker model name"
  value       = aws_sagemaker_model.chatbot.name
}

output "execution_role_arn" {
  description = "IAM role ARN used by SageMaker"
  value       = aws_iam_role.sagemaker_execution.arn
}

data "aws_region" "current" {}
