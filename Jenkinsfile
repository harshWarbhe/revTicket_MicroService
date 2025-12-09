pipeline {
    agent any
    
    environment {
        PATH = "/usr/local/bin:/usr/bin:/bin:${env.PATH}"
        DOCKER_HUB_USERNAME = 'harshwarbhe'
        EUREKA_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-eureka"
        GATEWAY_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-gateway"
        USER_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-user"
        MOVIE_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-movie"
        THEATER_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-theater"
        SHOWTIME_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-showtime"
        BOOKING_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-booking"
        PAYMENT_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-payment"
        REVIEW_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-review"
        SEARCH_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-search"
        NOTIFICATION_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-notification"
        SETTINGS_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-settings"
        DASHBOARD_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-dashboard"
        FRONTEND_IMAGE = "${DOCKER_HUB_USERNAME}/revticket-ms-frontend"
        BUILD_TAG = "${env.BUILD_NUMBER}"
    }
    
    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Pull Database Images') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker pull mysql:8.0'
                        sh 'docker pull mongo:7.0'
                    } else {
                        bat 'docker pull mysql:8.0'
                        bat 'docker pull mongo:7.0'
                    }
                }
            }
        }
        
        stage('Setup Buildx') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'docker buildx create --use --name multiarch-builder || docker buildx use multiarch-builder'
                        sh 'docker run --privileged --rm tonistiigi/binfmt --install all'
                        sh 'docker buildx inspect --bootstrap'
                    } else {
                        bat 'docker buildx create --use --name multiarch-builder || docker buildx use multiarch-builder'
                        bat 'docker buildx inspect --bootstrap'
                    }
                }
            }
        }
        
        stage('Build & Push Eureka Server') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                            dir('Microservices-Backend/eureka-server') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${EUREKA_IMAGE}:${BUILD_TAG} -t ${EUREKA_IMAGE}:latest --push ."
                            }
                        } else {
                            bat 'echo %DOCKER_PASS% | docker login -u %DOCKER_USER% --password-stdin'
                            dir('Microservices-Backend/eureka-server') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${EUREKA_IMAGE}:${BUILD_TAG} -t ${EUREKA_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push API Gateway') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/api-gateway') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${GATEWAY_IMAGE}:${BUILD_TAG} -t ${GATEWAY_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/api-gateway') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${GATEWAY_IMAGE}:${BUILD_TAG} -t ${GATEWAY_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push User Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/user-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${USER_IMAGE}:${BUILD_TAG} -t ${USER_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/user-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${USER_IMAGE}:${BUILD_TAG} -t ${USER_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Movie Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/movie-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${MOVIE_IMAGE}:${BUILD_TAG} -t ${MOVIE_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/movie-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${MOVIE_IMAGE}:${BUILD_TAG} -t ${MOVIE_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Theater Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/theater-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${THEATER_IMAGE}:${BUILD_TAG} -t ${THEATER_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/theater-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${THEATER_IMAGE}:${BUILD_TAG} -t ${THEATER_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Showtime Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/showtime-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${SHOWTIME_IMAGE}:${BUILD_TAG} -t ${SHOWTIME_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/showtime-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${SHOWTIME_IMAGE}:${BUILD_TAG} -t ${SHOWTIME_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Booking Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/booking-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${BOOKING_IMAGE}:${BUILD_TAG} -t ${BOOKING_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/booking-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${BOOKING_IMAGE}:${BUILD_TAG} -t ${BOOKING_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Payment Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/payment-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${PAYMENT_IMAGE}:${BUILD_TAG} -t ${PAYMENT_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/payment-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${PAYMENT_IMAGE}:${BUILD_TAG} -t ${PAYMENT_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Review Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/review-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${REVIEW_IMAGE}:${BUILD_TAG} -t ${REVIEW_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/review-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${REVIEW_IMAGE}:${BUILD_TAG} -t ${REVIEW_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Search Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/search-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${SEARCH_IMAGE}:${BUILD_TAG} -t ${SEARCH_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/search-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${SEARCH_IMAGE}:${BUILD_TAG} -t ${SEARCH_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Notification Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/notification-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${NOTIFICATION_IMAGE}:${BUILD_TAG} -t ${NOTIFICATION_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/notification-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${NOTIFICATION_IMAGE}:${BUILD_TAG} -t ${NOTIFICATION_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Settings Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/settings-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${SETTINGS_IMAGE}:${BUILD_TAG} -t ${SETTINGS_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/settings-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${SETTINGS_IMAGE}:${BUILD_TAG} -t ${SETTINGS_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Dashboard Service') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Microservices-Backend/dashboard-service') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${DASHBOARD_IMAGE}:${BUILD_TAG} -t ${DASHBOARD_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Microservices-Backend/dashboard-service') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${DASHBOARD_IMAGE}:${BUILD_TAG} -t ${DASHBOARD_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Build & Push Frontend') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'docker-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        if (isUnix()) {
                            dir('Frontend') {
                                sh "docker buildx build --platform linux/amd64,linux/arm64 -t ${FRONTEND_IMAGE}:${BUILD_TAG} -t ${FRONTEND_IMAGE}:latest --push ."
                            }
                        } else {
                            dir('Frontend') {
                                bat "docker buildx build --platform linux/amd64,linux/arm64 -t ${FRONTEND_IMAGE}:${BUILD_TAG} -t ${FRONTEND_IMAGE}:latest --push ."
                            }
                        }
                    }
                }
            }
        }
        
        stage('Verify Images') {
            steps {
                script {
                    if (isUnix()) {
                        sh "docker buildx imagetools inspect ${EUREKA_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${GATEWAY_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${USER_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${MOVIE_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${THEATER_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${SHOWTIME_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${BOOKING_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${PAYMENT_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${REVIEW_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${SEARCH_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${NOTIFICATION_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${SETTINGS_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${DASHBOARD_IMAGE}:latest"
                        sh "docker buildx imagetools inspect ${FRONTEND_IMAGE}:latest"
                    } else {
                        bat "docker buildx imagetools inspect ${EUREKA_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${GATEWAY_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${USER_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${MOVIE_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${THEATER_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${SHOWTIME_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${BOOKING_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${PAYMENT_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${REVIEW_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${SEARCH_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${NOTIFICATION_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${SETTINGS_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${DASHBOARD_IMAGE}:latest"
                        bat "docker buildx imagetools inspect ${FRONTEND_IMAGE}:latest"
                    }
                }
            }
        }
        
        stage('Deploy to EC2') {
            steps {
                script {
                    echo '🚀 Deploy on EC2:'
                    echo 'cd ~/revticket/Microservices-Backend'
                    echo 'docker-compose pull'
                    echo 'docker-compose up -d'
                }
            }
        }
    }
    
    post {
        always {
            script {
                if (isUnix()) {
                    sh 'docker system prune -f || true'
                } else {
                    bat 'docker system prune -f || exit 0'
                }
            }
        }
        success {
            echo "✅ Build #${BUILD_NUMBER} completed - 14 microservices images pushed"
        }
        failure {
            echo "❌ Build #${BUILD_NUMBER} failed"
        }
        cleanup {
            cleanWs()
        }
    }
}
