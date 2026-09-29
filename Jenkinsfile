pipeline {
    agent any

    environment {
        DOCKERHUB_USER = 'fangjae'
        PROD_HOST      = '13.124.40.206'
        IMAGE_TAG      = "${env.BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Images') {
            steps {
                sh "docker build -t ${DOCKERHUB_USER}/ilog-backend:${IMAGE_TAG} -t ${DOCKERHUB_USER}/ilog-backend:latest ./backend"
                sh "docker build -t ${DOCKERHUB_USER}/ilog-frontend:${IMAGE_TAG} -t ${DOCKERHUB_USER}/ilog-frontend:latest ./frontend"
            }
        }

        stage('Push Images') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DH_USER', passwordVariable: 'DH_TOKEN')]) {
                    sh 'echo "$DH_TOKEN" | docker login -u "$DH_USER" --password-stdin'
                    sh "docker push ${DOCKERHUB_USER}/ilog-backend:${IMAGE_TAG}"
                    sh "docker push ${DOCKERHUB_USER}/ilog-backend:latest"
                    sh "docker push ${DOCKERHUB_USER}/ilog-frontend:${IMAGE_TAG}"
                    sh "docker push ${DOCKERHUB_USER}/ilog-frontend:latest"
                }
            }
        }

        stage('Deploy') {
            steps {
                sshagent(credentials: ['prod-server-ssh']) {
                    sh """
                        ssh -o StrictHostKeyChecking=no ubuntu@${PROD_HOST} '
                            cd ~/iLog &&
                            git pull &&
                            docker compose -f docker-compose.prod.yml pull &&
                            docker compose -f docker-compose.prod.yml up -d
                        '
                    """
                }
            }
        }
    }

    post {
        always {
            sh 'docker logout || true'
        }
    }
}
