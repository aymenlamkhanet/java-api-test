# ELK Stack Integration for Jenkins CI/CD

## 📋 Overview

This setup integrates Jenkins pipeline logs with the ELK Stack (Elasticsearch, Logstash, Kibana) for centralized logging and visualization.

## 🚀 Quick Start

### 1. Start ELK Stack

```bash
cd elk
docker-compose up -d
```

### 2. Access Kibana

Open http://localhost:5601 in your browser.

### 3. Create Index Pattern

1. Go to **Stack Management** → **Index Patterns**
2. Create pattern: `jenkins-pipeline-logs-*`
3. Select `@timestamp` as time field

## 📊 What Gets Logged

The pipeline automatically sends structured logs for:

| Event | Data Included |
|-------|---------------|
| **Pipeline Start** | Job name, build number, git commit |
| **Stage Completion** | Stage name, status, duration |
| **Quality Gate** | Coverage, duplications, bugs, vulnerabilities |
| **Test Results** | Passed, failed, skipped counts |
| **Security Scan** | Critical, high, medium vulnerabilities |
| **Pipeline End** | Final status, total duration |

## 📈 Log Structure

Each log entry contains:

```json
{
  "@timestamp": "2026-02-06T15:30:00.000Z",
  "pipeline": "product-service",
  "build_number": "42",
  "build_url": "http://jenkins/job/product-service/42/",
  "git_commit": "abc1234",
  "git_branch": "main",
  "stage": "quality-gate",
  "status": "SUCCESS",
  "level": "INFO",
  "message": "Quality Gate PASSED",
  "duration_seconds": 45.5,
  "metrics": {
    "coverage": "95.3",
    "duplications": "0.0",
    "bugs": 0,
    "vulnerabilities": 0
  }
}
```

## 🔍 Useful Kibana Queries

### Find Failed Builds
```
status: "FAILED" AND level: "ERROR"
```

### Quality Gate Issues
```
stage: "quality-gate" AND status: "FAILED"
```

### Security Vulnerabilities
```
stage: "security-scan" AND vulnerabilities.critical: >0
```

### Slow Builds (>5 min)
```
stage: "final" AND duration_seconds: >300
```

## 📊 Dashboard Panels

Import the pre-built dashboard from `kibana/dashboards/jenkins-dashboard.ndjson`:

1. Go to **Stack Management** → **Saved Objects**
2. Click **Import**
3. Select the `.ndjson` file

### Included Visualizations

- **Pipeline Status Overview** - Pie chart of success/failure ratio
- **Build Duration Trend** - Line chart over time
- **Failures by Stage** - Bar chart showing which stages fail most
- **Quality Metrics Trend** - Coverage and duplications over time
- **Security Vulnerabilities** - Count of CVEs found

## 🔧 Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ELASTICSEARCH_URL` | `http://elasticsearch:9200` | Elasticsearch endpoint |
| `LOGSTASH_URL` | `http://logstash:5044` | Logstash HTTP input |
| `ELK_INDEX` | `jenkins-pipeline-logs` | Index name prefix |

### Logstash Pipeline

Edit `logstash/pipeline/jenkins.conf` to customize log processing.

## 🐛 Troubleshooting

### Logs not appearing in Kibana

1. Check Elasticsearch is running:
   ```bash
   curl http://localhost:9200/_cluster/health
   ```

2. Check if index exists:
   ```bash
   curl http://localhost:9200/_cat/indices?v
   ```

3. Verify logs are being sent:
   ```bash
   docker logs logstash
   ```

### Connection refused from Jenkins

Ensure Jenkins container can reach ELK network:
```bash
docker network connect elk-network jenkins
```

## 📝 Manual Log Sending

You can manually send a log entry for testing:

```bash
curl -X POST "http://localhost:9200/jenkins-pipeline-logs-$(date +%Y.%m.%d)/_doc" \
  -H "Content-Type: application/json" \
  -d '{
    "@timestamp": "'$(date -u +%Y-%m-%dT%H:%M:%S.000Z)'",
    "pipeline": "test",
    "stage": "manual-test",
    "status": "SUCCESS",
    "message": "Test log entry"
  }'
```

## 🔒 Security (Production)

For production, enable security:

1. Set `xpack.security.enabled: true` in Elasticsearch
2. Generate certificates for TLS
3. Create API keys for Jenkins
4. Use HTTPS endpoints
