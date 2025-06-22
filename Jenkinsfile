pipeline {
    agent any

    environment {
        EC2_HOST = '54.180.116.99'
        EC2_USER = 'ubuntu'
        DOCKER_IMAGE = 'ocean-app'
        DOCKER_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Prepare') {
            steps {
                echo '=== 프로젝트 준비 ==='
                sh """
                    echo "Current directory: \$(pwd)"
                    echo "Checking project directory..."
                    ls -la ${PROJECT_PATH}

                    # gradlew 확인
                    if [ -f ${PROJECT_PATH}/gradlew ]; then
                        echo "gradlew found!"
                    else
                        echo "ERROR: gradlew not found!"
                        exit 1
                    fi
                """
            }
        }

        stage('Build Application') {
            steps {
                echo '=== 애플리케이션 빌드 ==='
                sh """
                    cd ${PROJECT_PATH}
                    pwd
                    ls -la

                    # Gradle wrapper 권한 설정 및 실행
                    chmod +x ./gradlew
                    ./gradlew clean bootJar --no-daemon --stacktrace
                """
            }
        }

        stage('Run Tests') {
            steps {
                echo '=== 테스트 실행 ==='
                sh """
                    cd ${PROJECT_PATH}
                    ./gradlew test --no-daemon -x test
                """
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '=== Docker 이미지 빌드 ==='
                sh """
                    cd ${PROJECT_PATH}

                    # Docker 확인
                    which docker || echo "Docker not found in PATH"
                    docker --version || echo "Docker command failed"

                    # 이미지 빌드
                    docker build -t ${DOCKER_IMAGE}:${DOCKER_TAG} .
                    docker tag ${DOCKER_IMAGE}:${DOCKER_TAG} ${DOCKER_IMAGE}:latest
                """
            }
        }

        stage('Save Docker Image') {
            steps {
                echo '=== Docker 이미지 저장 ==='
                sh """
                    docker save ${DOCKER_IMAGE}:${DOCKER_TAG} | gzip > /tmp/${DOCKER_IMAGE}-${DOCKER_TAG}.tar.gz
                    ls -lh /tmp/${DOCKER_IMAGE}-${DOCKER_TAG}.tar.gz
                """
            }
        }

        stage('Deploy to EC2') {
            steps {
                echo '=== EC2 배포 시작 ==='
                sshagent(['ocean-ec2-ssh']) {
                    sh '''
                        # 변수 설정
                        IMAGE_FILE="/tmp/ocean-app-${BUILD_NUMBER}.tar.gz"

                        # 이미지 파일 전송 (환경 변수 사용)
                        echo "Transferring ${IMAGE_FILE} to EC2..."
                        scp -o StrictHostKeyChecking=no ${IMAGE_FILE} ${EC2_USER}@${EC2_HOST}:/tmp/

                        # EC2에서 배포 실행 (환경 변수 사용)
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} << EOF
                            # Docker 이미지 로드
                            echo "Loading Docker image..."
                            docker load < /tmp/ocean-app-${BUILD_NUMBER}.tar.gz

                            # 기존 컨테이너 중지 및 제거
                            docker stop ocean-app || true
                            docker rm ocean-app || true

                            # 새 컨테이너 실행
                            echo "Starting new container..."
                            docker run -d \
                                --name ocean-app \
                                --network ubuntu_ocean-network \
                                -p 8080:8080 \
                                --restart unless-stopped \
                                -e JAVA_OPTS="-Xmx256m -Xms128m -XX:+UseG1GC" \
                                ocean-app:${BUILD_NUMBER}

                            # 헬스체크
                            echo "Waiting for application to start..."
                            sleep 30
                            curl -f http://localhost:8080/actuator/health || echo "Health check warning - app may still be starting"

                            # 임시 파일 삭제
                            rm /tmp/ocean-app-${BUILD_NUMBER}.tar.gz

                            # 컨테이너 상태 확인
                            docker ps | grep ocean-app

                            # 로그 출력
                            echo "Recent logs:"
                            docker logs --tail 20 ocean-app
        EOF
                    '''
                }
            }
        }

    post {
        success {
            echo '✅ 파이프라인 성공!'
            echo "애플리케이션이 http://${EC2_HOST} 에서 실행 중입니다."
        }
        failure {
            echo '❌ 파이프라인 실패!'
            sh """
                echo "Debug information:"
                echo "Working directory: \$(pwd)"
                echo "Project path contents:"
                ls -la ${PROJECT_PATH} || true
            """
        }
        always {
            // 로컬 임시 파일 정리
            sh "rm -f /tmp/${DOCKER_IMAGE}-${DOCKER_TAG}.tar.gz || true"
        }
    }
}