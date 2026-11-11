pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://host.docker.internal:5433/medibook_test'
    }

    options {
        timestamps()
    }

    stages {
        stage('Prepare') {
            steps {
                echo 'Preparing MediBook build...'
                sh 'chmod +x mvnw'
            }
        }

        stage('Build User Service') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'medibook-db',
                        usernameVariable: 'DB_USERNAME',
                        passwordVariable: 'DB_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'medibook-admin',
                        usernameVariable: 'ADMIN_EMAIL',
                        passwordVariable: 'ADMIN_PASSWORD'
                    )
                ]) {
                    echo 'Building User Service...'
                    sh './mvnw -B -f user-service/pom.xml clean verify'
                }
            }
        }

        stage('Build Business Service') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'medibook-db',
                        usernameVariable: 'DB_USERNAME',
                        passwordVariable: 'DB_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'medibook-admin',
                        usernameVariable: 'ADMIN_EMAIL',
                        passwordVariable: 'ADMIN_PASSWORD'
                    )
                ]) {
                    echo 'Building Business Service...'
                    sh './mvnw -B -f business-service/pom.xml clean verify'
                }
            }
        }

        stage('Build Notification Service') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'medibook-db',
                        usernameVariable: 'DB_USERNAME',
                        passwordVariable: 'DB_PASSWORD'
                    ),
                    usernamePassword(
                        credentialsId: 'medibook-admin',
                        usernameVariable: 'ADMIN_EMAIL',
                        passwordVariable: 'ADMIN_PASSWORD'
                    )
                ]) {
                    echo 'Building Notification Service...'
                    sh './mvnw -B -f notification-service/pom.xml clean verify'
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo 'Building MediBook Docker images...'

                sh '''
                    set -eu

                    docker build \
                        -t medibook-user-service:${BUILD_NUMBER} \
                        -t medibook-user-service:latest \
                        ./user-service

                    docker build \
                        -t medibook-business-service:${BUILD_NUMBER} \
                        -t medibook-business-service:latest \
                        ./business-service

                    docker build \
                        -t medibook-notification-service:${BUILD_NUMBER} \
                        -t medibook-notification-service:latest \
                        ./notification-service

                    echo "Verifying Docker images..."

                    docker image inspect \
                        medibook-user-service:${BUILD_NUMBER} \
                        medibook-business-service:${BUILD_NUMBER} \
                        medibook-notification-service:${BUILD_NUMBER}

                    echo "All three Docker images built successfully!"
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: '**/target/*-reports/*.xml'
        }

        success {
            echo 'All MediBook services built and tested successfully!'
        }

        failure {
            echo 'MediBook CI pipeline failed. Check the build logs.'
        }
    }
}
