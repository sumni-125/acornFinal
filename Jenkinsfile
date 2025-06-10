pipeline {
    agent any

    environment {
        // 프로젝트 설정
        PROJECT_NAME = 'ocean'
        DOCKER_IMAGE = "ocean-app"
        DOCKER_TAG = "${BUILD_NUMBER}"

        // Slack 알림 설정 (선택사항)
        SLACK_CHANNEL = '#ocean-deploy'
    }

    tools {
        // Jenkins에서 설정한 도구 이름
        jdk 'JDK-17'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                script {
                    // Git 정보 가져오기
                    env.GIT_COMMIT = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                    env.GIT_BRANCH = sh(script: 'git rev-parse --abbrev-ref HEAD', returnStdout: true).trim()
                }
            }
        }

        stage('Build') {
            steps {
                sh '''
                    echo "Building Ocean application..."
                    chmod +x gradlew
                    ./gradlew clean build -x test
                '''
            }
        }

        stage('Test') {
            steps {
                sh '''
                    echo "Running tests..."
                    ./gradlew test
                '''
            }
            post {
                always {
                    // 테스트 결과 리포트
                    junit '**/build/test-results/test/*.xml'

                    // 코드 커버리지 리포트 (Jacoco 사용 시)
                    publishHTML([
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/tests/test',
                        reportFiles: 'index.html',
                        reportName: 'Test Report'
                    ])
                }
            }
        }

        stage('Code Quality') {
            when {
                branch 'develop'
            }
            steps {
                sh '''
                    echo "Running code quality checks..."
                    ./gradlew checkstyleMain
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        echo "Building Docker image..."
                        docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                        docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                    """
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                script {
                    sh """
                        echo "Deploying to development environment..."
                        # Docker Compose를 사용한 배포
                        cd docker
                        docker-compose stop ocean-app || true
                        docker-compose rm -f ocean-app || true
                        docker-compose up -d ocean-app
                    """
                }
            }
        }

        stage('Deploy to Production') {
            when {
                branch 'main'
            }
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'

                script {
                    sh """
                        echo "Deploying to production environment..."
                        # 프로덕션 배포 스크립트
                    """
                }
            }
        }
    }

    post {
        success {
            echo 'Pipeline succeeded!'
            // Slack 알림 (설정된 경우)
            script {
                if (env.SLACK_WEBHOOK) {
                    sh """
                        curl -X POST -H 'Content-type: application/json' \
                        --data '{"text":"✅ Build Successful: ${PROJECT_NAME} - Build #${BUILD_NUMBER} (${GIT_BRANCH})"}' \
                        ${SLACK_WEBHOOK}
                    """
                }
            }
        }

        failure {
            echo 'Pipeline failed!'
            // Slack 알림 (설정된 경우)
            script {
                if (env.SLACK_WEBHOOK) {
                    sh """
                        curl -X POST -H 'Content-type: application/json' \
                        --data '{"text":"❌ Build Failed: ${PROJECT_NAME} - Build #${BUILD_NUMBER} (${GIT_BRANCH})"}' \
                        ${SLACK_WEBHOOK}
                    """
                }
            }
        }

        always {
            // 작업 공간 정리
            cleanWs()
        }
    }
}