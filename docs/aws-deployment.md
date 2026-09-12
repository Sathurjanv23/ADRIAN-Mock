# AI Emergency Response & Relief Network (ADRN) — AWS Deployment Guide

This guide details the deployment of ADRN onto Amazon Web Services (AWS) using production best practices, infrastructure-as-code, and LocalStack for offline development.

---

## 1. Cloud Architecture Overview

```
                          [ Internet Users & First Responders ]
                                         │
                                   [ Route 53 ]
                                         │
                                [ CloudFront CDN ]
                                         │
                         [ Application Load Balancer ]
                         ┌───────────────┴───────────────┐
                         ▼                               ▼
                 [ ECS Fargate ]                  [ ECS Fargate ]
                 Next.js Frontend                 Spring Boot Backend
                   (Port 3000)                      (Port 8080)
                         │                               │
                         │                               ├─────► [ AWS Bedrock (Claude 3 Sonnet) ]
                         │                               ├─────► [ S3 Bucket (Media / Voice) ]
                         │                               ├─────► [ ElastiCache Redis ]
                         ▼                               ▼
               [ MongoDB Atlas on AWS ] ◄────────────────┘
```

---

## 2. Prerequisites

1. **AWS CLI v2** installed and configured (`aws configure`).
2. **Docker** and **Docker Compose** installed.
3. IAM identity with permissions to deploy ECS, ECR, IAM, S3, and Bedrock.
4. AWS Region with Claude 3 Sonnet enabled on Amazon Bedrock (`us-east-1` or `us-west-2` recommended).

---

## 3. Environment Configuration

### Production Variables (`application-prod.yml` / Secrets Manager)

| Variable | Description | Example / Recommended |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `MONGODB_URI` | Connection URI | `mongodb+srv://user:pass@cluster.mongodb.net/nova_db` |
| `JWT_SECRET` | 256-bit cryptographically secure key | Strong random 64-char string |
| `ADRN_AI_PROVIDER` | AI backend provider | `bedrock` (or `mock` for staging) |
| `AWS_REGION` | AWS Region hosting Bedrock | `us-east-1` |
| `BEDROCK_MODEL_ID` | Bedrock Foundation Model ID | `anthropic.claude-3-sonnet-20240229-v1:0` |
| `AWS_S3_BUCKET` | Bucket name for evidence & voice | `adrn-media-production` |

---

## 4. Step-by-Step Deployment

### 4.1. Build & Push Docker Images to ECR

```bash
# Authenticate Docker to AWS ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com

# 1. Build and push Spring Boot backend
cd project-nova/backend
docker build -t adrn-backend:latest .
docker tag adrn-backend:latest <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/adrn-backend:latest
docker push <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/adrn-backend:latest

# 2. Build and push Next.js frontend
cd ../frontend
docker build -t adrn-frontend:latest .
docker tag adrn-frontend:latest <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/adrn-frontend:latest
docker push <aws_account_id>.dkr.ecr.us-east-1.amazonaws.com/adrn-frontend:latest
```

### 4.2. IAM Roles & Least-Privilege Policy

Assign the IAM role defined in `infrastructure/aws/iam-policy.json` to the ECS Task Role:
- `bedrock:InvokeModel` on `arn:aws:bedrock:*::foundation-model/anthropic.claude-3-sonnet*`
- `s3:PutObject`, `s3:GetObject` on `arn:aws:s3:::adrn-media-production/*`
- `logs:CreateLogStream`, `logs:PutLogEvents` on CloudWatch.

### 4.3. ECS Task Definitions & Service Deployment

- Allocate `1 vCPU / 2GB RAM` for the Next.js frontend container.
- Allocate `2 vCPU / 4GB RAM` for the Spring Boot backend container.
- Enable CloudWatch Container Insights for distributed metrics and log correlation.
- Target group health check: `/actuator/health` on port `8080`.

---

## 5. Offline Local Testing with LocalStack

For zero-cloud-cost testing, use the included `docker-compose.yml` which initializes LocalStack:

```bash
cd project-nova/infrastructure/docker
docker-compose up -d localstack
```

Verify LocalStack services:
```bash
aws --endpoint-url=http://localhost:4566 s3 ls
```
