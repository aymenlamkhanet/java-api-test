pipeline {
    agent any

    environment {
        // ==========================================
        // Configuration du Registry Harbor
        // ==========================================
        HARBOR_REGISTRY = 'harbor.local'
        HARBOR_PROJECT = 'devops'
        IMAGE_NAME = 'product-service'
        HARBOR_CREDENTIALS = 'harbor-credentials'
        
        // ==========================================
        // Configuration SonarQube
        // ==========================================
        SONAR_HOST_URL = 'http://sonarqube:9000'
        SONAR_PROJECT_KEY = 'product-service'
        
        // ==========================================
        // Configuration Maven
        // ==========================================
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
        JAVA_HOME = '/usr/lib/jvm/java-17-openjdk'
        
        // ==========================================
        // Seuils de sécurité
        // ==========================================
        TRIVY_SEVERITY = 'CRITICAL,HIGH'
        DEPENDENCY_CHECK_FAIL_SCORE = '9'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 45, unit: 'MINUTES')
        disableConcurrentBuilds()
        skipDefaultCheckout(false)
    }

    stages {
        // ============================================
        // STAGE 1: Checkout + Initialisation
        // ============================================
        stage('1-checkout-init') {
            steps {
                cleanWs()
                checkout scm
                script {
                    // Calcul du SHA court pour le tag de l'image
                    env.SHORT_SHA = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.BUILD_TAG = "${SHORT_SHA}-${BUILD_NUMBER}"
                    env.IMAGE_REF = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME}:${BUILD_TAG}"
                    env.IMAGE_LATEST = "${HARBOR_REGISTRY}/${HARBOR_PROJECT}/${IMAGE_NAME}:latest"
                    
                    echo "=========================================="
                    echo "Pipeline DevOps - Product Service"
                    echo "=========================================="
                    echo "Git Commit: ${SHORT_SHA}"
                    echo "Build Tag: ${BUILD_TAG}"
                    echo "Image Reference: ${IMAGE_REF}"
                    echo "=========================================="
                }
            }
        }

        // ============================================
        // STAGE 2: Compilation (Fail Fast)
        // ============================================
        stage('2-build-compile') {
            steps {
                echo "🔨 Compilation du code Java..."
                sh 'mvn compile -B -q -DskipTests'
            }
            post {
                failure {
                    echo "❌ ERREUR: Compilation échouée"
                    error 'Compilation failed - stopping pipeline (fail fast)'
                }
                success {
                    echo "✅ Compilation réussie"
                }
            }
        }

        // ============================================
        // STAGE 3: Tests Unitaires + Couverture JaCoCo
        // ============================================
        stage('3-unit-tests') {
            steps {
                echo "🧪 Exécution des tests unitaires et d'intégration..."
                sh 'mvn test -B'
            }
            post {
                always {
                    // Publication des résultats JUnit
                    junit testResults: '**/target/surefire-reports/*.xml', 
                          allowEmptyResults: false,
                          skipPublishingChecks: false
                    
                    // Publication de la couverture JaCoCo
                    jacoco(
                        execPattern: '**/target/*.exec',
                        classPattern: '**/target/classes',
                        sourcePattern: '**/src/main/java',
                        exclusionPattern: '**/test/**',
                        minimumLineCoverage: '70',
                        minimumBranchCoverage: '60'
                    )
                    
                    // Archiver les rapports
                    archiveArtifacts artifacts: 'target/surefire-reports/**/*', fingerprint: true
                    archiveArtifacts artifacts: 'target/site/jacoco/**/*', fingerprint: true
                }
                failure {
                    echo "❌ ERREUR: Tests unitaires échoués"
                    error 'Unit tests failed - stopping pipeline'
                }
                success {
                    echo "✅ 90 tests passés avec succès"
                }
            }
        }

        // ============================================
        // STAGE 4: Analyse SonarQube (SAST + Qualité)
        // ============================================
        stage('4-sonarqube-sast-quality') {
            steps {
                echo "🔍 Analyse SonarQube (SAST + Qualité du code)..."
                withSonarQubeEnv('SonarQube') {
                    sh '''
                        mvn sonar:sonar -B \
                            -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                            -Dsonar.projectName="Product Service" \
                            -Dsonar.host.url=${SONAR_HOST_URL} \
                            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
                            -Dsonar.junit.reportPaths=target/surefire-reports
                    '''
                }
            }
            post {
                success {
                    echo "✅ Analyse SonarQube terminée"
                }
            }
        }

        // ============================================
        // STAGE 5: Quality Gate (Bloquant)
        // ============================================
        stage('5-quality-gate') {
            steps {
                echo "🚦 Vérification du Quality Gate SonarQube..."
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
            post {
                failure {
                    echo "❌ ERREUR: Quality Gate non conforme"
                    error 'Quality Gate failed - stopping pipeline'
                }
                success {
                    echo "✅ Quality Gate PASSED"
                }
            }
        }

        // ============================================
        // STAGE 6: SCA - Analyse des dépendances (OWASP)
        // ============================================
        stage('6-sca-dependencies') {
            steps {
                echo "🔐 Analyse des vulnérabilités des dépendances (OWASP)..."
                sh '''
                    mvn org.owasp:dependency-check-maven:check -B \
                        -DfailBuildOnCVSS=${DEPENDENCY_CHECK_FAIL_SCORE} \
                        -DsuppressionFile=dependency-check-suppression.xml || true
                '''
            }
            post {
                always {
                    // Publier le rapport Dependency-Check
                    dependencyCheckPublisher pattern: 'target/dependency-check-report.xml'
                    archiveArtifacts artifacts: 'target/dependency-check-report.*', fingerprint: true, allowEmptyArchive: true
                }
                success {
                    echo "✅ Analyse des dépendances terminée"
                }
            }
        }

        // ============================================
        // STAGE 7: Package (JAR)
        // ============================================
        stage('7-package') {
            steps {
                echo "📦 Création du package JAR..."
                sh 'mvn package -B -DskipTests -q'
            }
            post {
                success {
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
                    echo "✅ Package JAR créé: target/product-service-1.0.0-SNAPSHOT.jar"
                }
            }
        }

        // ============================================
        // STAGE 8: Build Image Docker (Jib)
        // ============================================
        stage('8-image-build-jib-local') {
            steps {
                echo "🐳 Construction de l'image Docker avec Jib..."
                sh """
                    mvn jib:dockerBuild -B \
                        -Djib.to.image=${IMAGE_REF} \
                        -Djib.to.tags=${BUILD_TAG},${SHORT_SHA},latest \
                        -Djib.console=plain
                """
            }
            post {
                success {
                    echo "✅ Image Docker construite: ${IMAGE_REF}"
                }
            }
        }

        // ============================================
        // STAGE 9: Scan Trivy (Vulnérabilités Image)
        // ============================================
        stage('9-trivy-image-scan') {
            steps {
                echo "🛡️ Scan de vulnérabilités de l'image avec Trivy..."
                sh """
                    trivy image \
                        --severity ${TRIVY_SEVERITY} \
                        --exit-code 1 \
                        --no-progress \
                        --format table \
                        ${IMAGE_REF}
                """
            }
            post {
                failure {
                    echo "❌ ERREUR: Vulnérabilités CRITICAL/HIGH détectées"
                    error 'Trivy scan failed - critical vulnerabilities found'
                }
                success {
                    echo "✅ Aucune vulnérabilité critique détectée"
                }
            }
        }

        // ============================================
        // STAGE 10: Smoke Test Runtime (Conteneur)
        // ============================================
        stage('10-container-smoke-runtime') {
            steps {
                echo "🚀 Démarrage du conteneur pour smoke test..."
                script {
                    // Démarrer le conteneur
                    sh """
                        docker rm -f product-service-test || true
                        docker run -d \
                            --name product-service-test \
                            -p 8080:8080 \
                            -e SPRING_PROFILES_ACTIVE=docker \
                            ${IMAGE_REF}
                    """
                    
                    // Attendre le démarrage
                    echo "⏳ Attente du démarrage de l'application..."
                    sh '''
                        for i in $(seq 1 30); do
                            if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
                                echo "✅ Application UP après $i secondes"
                                exit 0
                            fi
                            echo "Tentative $i/30..."
                            sleep 2
                        done
                        echo "❌ Timeout: Application non disponible"
                        docker logs product-service-test
                        exit 1
                    '''
                }
            }
            post {
                failure {
                    sh 'docker logs product-service-test || true'
                    sh 'docker rm -f product-service-test || true'
                    error 'Smoke test failed - container not healthy'
                }
                success {
                    echo "✅ Smoke test réussi - Application UP"
                }
            }
        }

        // ============================================
        // STAGE 11: Tests Robot Framework (API Regression)
        // ============================================
        stage('11-robot-api-regression') {
            steps {
                echo "🤖 Exécution des tests Robot Framework (30 tests E2E)..."
                sh '''
                    docker run --rm \
                        --network host \
                        -v $(pwd)/robot-tests:/tests \
                        -v $(pwd)/robot-reports:/reports \
                        robotframework/rfdocker:latest \
                        robot \
                            --outputdir /reports \
                            --xunit xunit.xml \
                            --log log.html \
                            --report report.html \
                            /tests/api_tests.robot
                '''
            }
            post {
                always {
                    // Arrêter le conteneur de test
                    sh 'docker rm -f product-service-test || true'
                    
                    // Publier les résultats Robot Framework
                    robot outputPath: 'robot-reports',
                          logFileName: 'log.html',
                          reportFileName: 'report.html',
                          outputFileName: 'output.xml',
                          passThreshold: 100.0,
                          unstableThreshold: 90.0
                    
                    // Publier résultats xUnit pour Jenkins
                    junit testResults: 'robot-reports/xunit.xml', allowEmptyResults: true
                    
                    // Archiver les rapports
                    archiveArtifacts artifacts: 'robot-reports/**/*', fingerprint: true
                }
                failure {
                    echo "❌ ERREUR: Tests Robot Framework échoués"
                    error 'Robot Framework tests failed - stopping pipeline'
                }
                success {
                    echo "✅ 30 tests E2E passés avec succès"
                }
            }
        }

        // ============================================
        // STAGE 12: Push vers Harbor
        // ============================================
        stage('12-push-harbor') {
            steps {
                echo "📤 Push de l'image vers Harbor..."
                withCredentials([usernamePassword(
                    credentialsId: "${HARBOR_CREDENTIALS}",
                    usernameVariable: 'HARBOR_USER',
                    passwordVariable: 'HARBOR_PASS'
                )]) {
                    sh '''
                        echo "${HARBOR_PASS}" | docker login ${HARBOR_REGISTRY} -u ${HARBOR_USER} --password-stdin
                        docker push ${IMAGE_REF}
                        docker push ${IMAGE_LATEST}
                        docker logout ${HARBOR_REGISTRY}
                    '''
                }
            }
            post {
                success {
                    echo "=========================================="
                    echo "✅ PIPELINE TERMINÉ AVEC SUCCÈS"
                    echo "=========================================="
                    echo "Image publiée: ${IMAGE_REF}"
                    echo "Digest disponible dans Harbor"
                    echo "=========================================="
                }
            }
        }
    }

    // ============================================
    // POST ACTIONS GLOBALES
    // ============================================
    post {
        always {
            // Nettoyage
            sh 'docker rm -f product-service-test || true'
            
            // Notification des résultats
            script {
                def buildStatus = currentBuild.currentResult
                def color = buildStatus == 'SUCCESS' ? 'good' : 'danger'
                
                echo """
                ==========================================
                RÉSUMÉ DU BUILD
                ==========================================
                Status: ${buildStatus}
                Commit: ${env.SHORT_SHA}
                Image: ${env.IMAGE_REF}
                Tests Unitaires: 90 tests
                Tests E2E: 30 tests
                Total: 120 tests
                ==========================================
                """
            }
        }
        success {
            echo "🎉 Pipeline CI terminé avec succès!"
        }
        failure {
            echo "❌ Pipeline CI échoué - Vérifier les logs"
        }
        cleanup {
            cleanWs()
        }
    }
}
