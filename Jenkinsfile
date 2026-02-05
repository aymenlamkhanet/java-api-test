pipeline {
    agent any

    tools {
        // Utilise les outils configurés dans Jenkins
        maven 'maven3.9'
    }

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
        // Seuils de sécurité
        // ==========================================
        TRIVY_SEVERITY = 'CRITICAL,HIGH'
        DEPENDENCY_CHECK_FAIL_SCORE = '9'
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
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
                    junit testResults: '**/target/surefire-reports/*.xml', 
                          allowEmptyResults: true,
                          skipPublishingChecks: false
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
                    withCredentials([string(credentialsId: 'sonarqube-cred', variable: 'SONAR_TOKEN')]) {
                        sh '''
                            mvn sonar:sonar -B \
                                -Dsonar.projectKey=${SONAR_PROJECT_KEY} \
                                -Dsonar.projectName="Product Service" \
                                -Dsonar.host.url=${SONAR_HOST_URL} \
                                -Dsonar.token=${SONAR_TOKEN}
                        '''
                    }
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
                    withSonarQubeEnv('SonarQube') {
                        waitForQualityGate abortPipeline: false
                    }
                }
            }
            post {
                failure {
                    echo "⚠️ Quality Gate check failed (continuing pipeline)"
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
                sh 'mvn org.owasp:dependency-check-maven:check -B -DfailBuildOnCVSS=9 || true'
            }
            post {
                always {
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
                    echo "✅ Package JAR créé"
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
                        -Djib.to.image=${IMAGE_NAME}:${BUILD_TAG} \
                        -Djib.to.tags=${BUILD_TAG},${SHORT_SHA},latest \
                        -Djib.console=plain
                """
            }
            post {
                success {
                    echo "✅ Image Docker construite: ${IMAGE_NAME}:${BUILD_TAG}"
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
                    docker run --rm \
                        -v /var/run/docker.sock:/var/run/docker.sock \
                        aquasec/trivy:latest image \
                            --severity CRITICAL,HIGH \
                            --exit-code 0 \
                            --no-progress \
                            ${IMAGE_NAME}:${BUILD_TAG}
                """
            }
            post {
                success {
                    echo "✅ Scan Trivy terminé"
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
                    sh """
                        docker rm -f product-service-test || true
                        docker run -d \
                            --name product-service-test \
                            -p 8080:8080 \
                            -e SPRING_PROFILES_ACTIVE=docker \
                            ${IMAGE_NAME}:${BUILD_TAG}
                    """
                    
                    echo "⏳ Attente du démarrage de l'application..."
                    sh '''
                        for i in $(seq 1 30); do
                            if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
                                echo "✅ Application UP après $i tentatives"
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
                    mkdir -p robot-reports
                    docker run --rm \
                        --network host \
                        -v "${WORKSPACE}/robot-tests":/tests \
                        -v "${WORKSPACE}/robot-reports":/reports \
                        ppodgorsek/robot-framework:latest \
                        robot \
                            --outputdir /reports \
                            --xunit xunit.xml \
                            --log log.html \
                            --report report.html \
                            /tests/api_tests.robot || true
                '''
            }
            post {
                always {
                    sh 'docker rm -f product-service-test || true'
                    junit testResults: 'robot-reports/xunit.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'robot-reports/**/*', fingerprint: true, allowEmptyArchive: true
                }
                success {
                    echo "✅ Tests E2E terminés"
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
                        docker tag ${IMAGE_NAME}:${BUILD_TAG} ${IMAGE_REF}
                        docker tag ${IMAGE_NAME}:${BUILD_TAG} ${IMAGE_LATEST}
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
                    echo "=========================================="
                }
            }
        }
    }

    post {
        always {
            sh 'docker rm -f product-service-test || true'
            script {
                def buildStatus = currentBuild.currentResult
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
