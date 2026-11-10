pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }
    environment {
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://host.docker.internal:5432/medibook_test'
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
                withCredentials([usernamePassword(
                    credentialsId: 'medibook-db',
                    usernameVariable: 'DB_USERNAME',
                    passwordVariable: 'DB_PASSWORD'
                )]) {
                    echo 'Building User Service...'
                    sh './mvnw -B -f user-service/pom.xml clean verify'
                }
            }
        }

        stage('Build Business Service') {
                    steps {
                        withCredentials([usernamePassword(
                            credentialsId: 'medibook-db',
                            usernameVariable: 'DB_USERNAME',
                            passwordVariable: 'DB_PASSWORD'
                        )]) {
                            echo 'Building Business Service...'
                            sh './mvnw -B -f business-service/pom.xml clean verify'
                        }
                    }
                }

         stage('Build Notification Service') {
                            steps {
                                withCredentials([usernamePassword(
                                    credentialsId: 'medibook-db',
                                    usernameVariable: 'DB_USERNAME',
                                    passwordVariable: 'DB_PASSWORD'
                                )]) {
                                    echo 'Building Notification Service...'
                                    sh './mvnw -B -f notification-service/pom.xml clean verify'
                                }
                            }
                        }


    post {
        always {
            junit allowEmptyResults: true,
                  testResults: '**/target/*-reports/*.xml'
        }

        success {
            echo 'All MediBook services built successfully!'
        }

        failure {
            echo 'MediBook CI pipeline failed. Check the build logs.'
        }
    }
}
