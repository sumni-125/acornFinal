pipeline {
    agent any

    environment {
        PROJECT_PATH = '/workspace/ocean'
        EC2_HOST = '13.209.22.5'
        EC2_USER = 'ubuntu'
    }

    stages {
        stage('Prepare') {
            steps {
                echo '=== 프로젝트 준비 ==='
                sh '''
                    pwd
                    echo "Current directory: $(pwd)"
                    echo "Checking project directory..."
                    ls -la ${PROJECT_PATH}

                    if [ -f "${PROJECT_PATH}/gradlew" ]; then
                        echo "gradlew found!"
                    else
                        echo "ERROR: gradlew not found!"
                        exit 1
                    fi
                '''
            }
        }

        stage('Build Application') {
            steps {
                echo '=== 애플리케이션 빌드 ==='
                sh '''
                    cd ${PROJECT_PATH}
                    pwd
                    ls -la
                    chmod +x ./gradlew
                    ./gradlew clean bootJar --no-daemon --stacktrace
                '''
            }
        }

        stage('Run Tests') {
            steps {
                echo '=== 테스트 실행 ==='
                sh '''
                    cd ${PROJECT_PATH}
                    ./gradlew test --no-daemon
                '''
            }
        }

        stage('Build Docker Image') {
            steps {
                echo '=== Docker 이미지 빌드 ==='
                sh '''
                    cd ${PROJECT_PATH}
                    which docker
                    docker --version
                    docker build -t ocean-app:${BUILD_NUMBER} .
                '''
            }
        }

        stage('Save Docker Image') {
            steps {
                echo '=== Docker 이미지 저장 ==='
                sh '''
                    docker save ocean-app:${BUILD_NUMBER} | gzip > /tmp/ocean-app-${BUILD_NUMBER}.tar.gz
                    ls -lh /tmp/ocean-app-${BUILD_NUMBER}.tar.gz
                '''
            }
        }

        stage('Deploy to EC2') {
            steps {
                echo '=== EC2 배포 시작 ==='
                sshagent(['ocean-ec2-ssh']) {
                    sh '''
                        # 이미지 파일 전송
                        scp -o StrictHostKeyChecking=no /tmp/ocean-app-${BUILD_NUMBER}.tar.gz ${EC2_USER}@${EC2_HOST}:/tmp/

                        # EC2에서 배포 실행
                        ssh -o StrictHostKeyChecking=no ${EC2_USER}@${EC2_HOST} << 'EOF'
                            # Docker 이미지 로드
                            docker load < /tmp/ocean-app-${BUILD_NUMBER}.tar.gz

                            # 기존 컨테이너 중지 및 제거
                            docker stop ocean-app || true
                            docker rm ocean-app || true

                            # 새 컨테이너 실행
                            docker run -d \
                                --name ocean-app \
                                -p 8080:8080 \
                                --restart unless-stopped \
                                ocean-app:${BUILD_NUMBER}

                            # 헬스체크
                            sleep 10
                            curl -f http://localhost:8080/actuator/health || echo "Health check failed"

                            # 임시 파일 삭제
                            rm /tmp/ocean-app-${BUILD_NUMBER}.tar.gz

                            # 컨테이너 상태 확인
                            docker ps | grep ocean-app
                        EOF
                    '''
                }
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
            sh '''
                echo "Debug information:"
                echo "Working directory: $(pwd)"
                echo "Project path contents:"
                ls -la ${PROJECT_PATH} || true
            '''
        }
        always {
            // 로컬 임시 파일 정리
            sh 'rm -f /tmp/ocean-app-${BUILD_NUMBER}.tar.gz || true'
        }
    }
}