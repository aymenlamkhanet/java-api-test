#!/usr/bin/env groovy

/**
 * Librairie partagée pour l'envoi de logs structurés vers ELK Stack
 * 
 * Usage dans Jenkinsfile:
 *   elkLog(stage: 'build', status: 'SUCCESS', message: 'Build completed')
 */

def call(Map config = [:]) {
    def elkUrl = env.ELASTICSEARCH_URL ?: 'http://elasticsearch:9200'
    def index = env.ELK_INDEX ?: 'jenkins-pipeline-logs'
    
    def timestamp = new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    
    def logEntry = [
        '@timestamp': timestamp,
        'pipeline': env.JOB_NAME ?: 'unknown',
        'build_number': env.BUILD_NUMBER ?: '0',
        'build_url': env.BUILD_URL ?: '',
        'git_commit': env.GIT_COMMIT ?: env.SHORT_SHA ?: 'unknown',
        'git_branch': env.GIT_BRANCH ?: 'unknown',
        'stage': config.stage ?: 'unknown',
        'status': config.status ?: 'INFO',
        'level': config.level ?: 'INFO',
        'message': config.message ?: '',
        'duration_ms': config.duration ?: 0,
        'metrics': config.metrics ?: [:],
        'test_results': config.testResults ?: [:],
        'vulnerabilities': config.vulnerabilities ?: [:],
        'node': env.NODE_NAME ?: 'master',
        'executor': env.EXECUTOR_NUMBER ?: '0'
    ]
    
    def jsonPayload = groovy.json.JsonOutput.toJson(logEntry)
    
    try {
        sh(script: """
            curl -s -X POST "${elkUrl}/${index}-\$(date +%Y.%m.%d)/_doc" \\
                -H "Content-Type: application/json" \\
                -d '${jsonPayload}'
        """, returnStdout: true)
        
        echo "📊 Log envoyé à ELK: [${config.stage}] ${config.status}"
    } catch (Exception e) {
        echo "⚠️ Erreur envoi ELK (non bloquant): ${e.message}"
    }
}

/**
 * Envoyer un log de début de stage
 */
def stageStart(String stageName) {
    call(
        stage: stageName,
        status: 'STARTED',
        level: 'INFO',
        message: "Stage '${stageName}' démarré"
    )
}

/**
 * Envoyer un log de fin de stage avec succès
 */
def stageSuccess(String stageName, Map additionalData = [:]) {
    def data = [
        stage: stageName,
        status: 'SUCCESS',
        level: 'INFO',
        message: "Stage '${stageName}' terminé avec succès"
    ] + additionalData
    call(data)
}

/**
 * Envoyer un log d'échec de stage
 */
def stageFailure(String stageName, String errorMessage = '') {
    call(
        stage: stageName,
        status: 'FAILED',
        level: 'ERROR',
        message: "Stage '${stageName}' échoué: ${errorMessage}"
    )
}

/**
 * Envoyer les métriques SonarQube
 */
def sonarMetrics(Map metrics) {
    call(
        stage: 'quality-gate',
        status: metrics.status ?: 'UNKNOWN',
        level: metrics.status == 'OK' ? 'INFO' : 'ERROR',
        message: "Quality Gate: ${metrics.status}",
        metrics: [
            coverage: metrics.coverage,
            duplications: metrics.duplications,
            bugs: metrics.bugs,
            vulnerabilities: metrics.vulnerabilities,
            codeSmells: metrics.codeSmells,
            securityHotspots: metrics.securityHotspots
        ]
    )
}

/**
 * Envoyer les résultats de tests
 */
def testResults(Map results) {
    call(
        stage: 'unit-tests',
        status: results.failed > 0 ? 'FAILED' : 'SUCCESS',
        level: results.failed > 0 ? 'ERROR' : 'INFO',
        message: "Tests: ${results.passed} passed, ${results.failed} failed, ${results.skipped} skipped",
        testResults: results
    )
}

/**
 * Envoyer les résultats de scan de sécurité
 */
def securityScan(Map results) {
    def status = (results.critical > 0 || results.high > 0) ? 'FAILED' : 'SUCCESS'
    call(
        stage: 'security-scan',
        status: status,
        level: status == 'FAILED' ? 'ERROR' : 'INFO',
        message: "Security Scan: ${results.critical} critical, ${results.high} high, ${results.medium} medium",
        vulnerabilities: results
    )
}

return this
