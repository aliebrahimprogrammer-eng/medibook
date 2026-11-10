pipeline {
    agent any

    tools {
        jdk 'jdk17'
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
                echo 'Building User Service...'
                sh './mvnw -B -f user-service/pom.xml clean verify'
            }
        }

        stage('Build Business Service') {
            steps {
                echo 'Building Business Service...'
                sh './mvnw -B -f business-service/pom.xml clean verify'
            }
        }

        stage('Build Notification Service') {
            steps {
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
