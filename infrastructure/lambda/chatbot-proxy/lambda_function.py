"""
Lambda function that proxies requests from API Gateway to SageMaker Endpoint.
Converts the simple { "question": "..." } format to SageMaker TGI format
and returns a clean response.
"""
import json
import os
import boto3

sagemaker_runtime = boto3.client("sagemaker-runtime")
ENDPOINT_NAME = os.environ.get("SAGEMAKER_ENDPOINT", "hms-production-chatbot")


def lambda_handler(event, context):
    """Handle API Gateway request → SageMaker → Response."""
    try:
        # Parse the incoming request
        if isinstance(event.get("body"), str):
            body = json.loads(event["body"])
        else:
            body = event.get("body") or event

        question = body.get("question", "")

        if not question:
            return {
                "statusCode": 400,
                "headers": cors_headers(),
                "body": json.dumps({"error": "Missing 'question' field"})
            }

        # Format for our custom FastAPI chatbot (expects {"question": "..."})
        payload = {
            "question": question
        }

        # Invoke SageMaker endpoint
        response = sagemaker_runtime.invoke_endpoint(
            EndpointName=ENDPOINT_NAME,
            ContentType="application/json",
            Body=json.dumps(payload)
        )

        # Parse SageMaker response (our custom app returns full JSON)
        result = json.loads(response["Body"].read().decode())

        # Our app returns: {"question":..., "answer":..., "source":..., ...}
        answer = result.get("answer", "")

        return {
            "statusCode": 200,
            "headers": cors_headers(),
            "body": json.dumps(result, ensure_ascii=False)
        }

    except sagemaker_runtime.exceptions.ModelError as e:
        return {
            "statusCode": 502,
            "headers": cors_headers(),
            "body": json.dumps({"error": "AI model error", "detail": str(e)})
        }
    except Exception as e:
        return {
            "statusCode": 500,
            "headers": cors_headers(),
            "body": json.dumps({"error": "Internal error", "detail": str(e)})
        }


def cors_headers():
    """CORS headers for browser access."""
    return {
        "Content-Type": "application/json",
        "Access-Control-Allow-Origin": "*",
        "Access-Control-Allow-Methods": "POST, OPTIONS",
        "Access-Control-Allow-Headers": "Content-Type"
    }
