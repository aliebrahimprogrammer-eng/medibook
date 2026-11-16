pipeline {
    agent any

    tools {
        jdk 'jdk17'
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5433/medibook_test'
        AWS_REGION = 'eu-north-1'
        ECR_REGISTRY = '798645002363.dkr.ecr.eu-north-1.amazonaws.com'
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

        stage('Push Images to AWS ECR') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-ecr-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        #!/bin/bash
                        set -euo pipefail

                        echo "Authenticating with AWS ECR..."

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            ecr get-login-password --region "$AWS_REGION" \
                            | docker login \
                                --username AWS \
                                --password-stdin "$ECR_REGISTRY"

                        trap 'docker logout "$ECR_REGISTRY" >/dev/null 2>&1 || true' EXIT

                        for service in user-service business-service notification-service
                        do
                            REPOSITORY="medibook-${service}"
                            LOCAL_IMAGE="medibook-${service}:${BUILD_NUMBER}"

                            echo "Tagging ${REPOSITORY}..."

                            docker tag "$LOCAL_IMAGE" \
                                "$ECR_REGISTRY/$REPOSITORY:${BUILD_NUMBER}"

                            docker tag "$LOCAL_IMAGE" \
                                "$ECR_REGISTRY/$REPOSITORY:latest"

                            echo "Pushing ${REPOSITORY}:${BUILD_NUMBER}..."
                            docker push "$ECR_REGISTRY/$REPOSITORY:${BUILD_NUMBER}"

                            echo "Pushing ${REPOSITORY}:latest..."
                            docker push "$ECR_REGISTRY/$REPOSITORY:latest"
                        done

                        echo "All three MediBook services pushed successfully to AWS ECR!"
                    '''
                }
            }
        }

        stage('Verify Elastic Beanstalk Access') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-ecr-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        set -eu

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk describe-environments \
                            --environment-names Medibook-env \
                            --query "Environments[0].[ApplicationName,EnvironmentName,VersionLabel,Status,Health]" \
                            --output table
                    '''
                }
            }
        }


        stage('Package Elastic Beanstalk Deployment') {
            steps {
                echo 'Preparing Elastic Beanstalk deployment package...'

                sh '''
                    set -eu

                    rm -rf deployment-package
                    mkdir -p deployment-package

                    cp -R aws-deployment/. deployment-package/

                    # Frontend files are not part of this backend deployment.
                    rm -rf deployment-package/frontend
                    rm -f deployment-package/.env.example

                    # Use the current Jenkins build number for all service images.
                    sed -i "s/:28$/:${BUILD_NUMBER}/" \
                        deployment-package/docker-compose.yml

                    echo "Checking deployment image tags..."
                    grep 'image:' deployment-package/docker-compose.yml

                    # Create ZIP with the correct Elastic Beanstalk root structure.
                    docker run --rm \
                        -v "$PWD:/work" \
                        -w /work \
                        alpine:3.21 \
                        sh -c "apk add --no-cache zip && cd deployment-package && zip -r ../medibook-eb-${BUILD_NUMBER}.zip ."

                    echo "Deployment package created."
                '''
            }
        }

        stage('Deploy to Elastic Beanstalk') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'aws-ecr-credentials',
                        usernameVariable: 'AWS_ACCESS_KEY_ID',
                        passwordVariable: 'AWS_SECRET_ACCESS_KEY'
                    )
                ]) {
                    sh '''
                        set -eu

                        VERSION_LABEL="medibook-${BUILD_NUMBER}"
                        S3_KEY="deployments/${VERSION_LABEL}.zip"

                        echo "Finding Elastic Beanstalk deployment bucket..."

                        S3_BUCKET=$(docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk create-storage-location \
                            --query S3Bucket \
                            --output text)

                        echo "Uploading deployment package to S3..."

                        docker run --rm \
                            -v "$PWD:/work" \
                            -w /work \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            s3 cp "medibook-eb-${BUILD_NUMBER}.zip" \
                            "s3://${S3_BUCKET}/${S3_KEY}"

                        echo "Creating Elastic Beanstalk application version..."

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk create-application-version \
                            --application-name medibook \
                            --version-label "$VERSION_LABEL" \
                            --source-bundle S3Bucket="$S3_BUCKET",S3Key="$S3_KEY"

                        echo "Deploying ${VERSION_LABEL}..."

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk update-environment \
                            --environment-name Medibook-env \
                            --version-label "$VERSION_LABEL"

                        echo "Waiting for deployment to finish..."

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk wait environment-updated \
                            --environment-names Medibook-env

                        echo "Checking deployment health..."

                        docker run --rm \
                            -e AWS_ACCESS_KEY_ID \
                            -e AWS_SECRET_ACCESS_KEY \
                            -e AWS_DEFAULT_REGION="$AWS_REGION" \
                            public.ecr.aws/aws-cli/aws-cli:latest \
                            elasticbeanstalk describe-environments \
                            --environment-names Medibook-env \
                            --query "Environments[0].[VersionLabel,Status,Health]" \
                            --output table
                    '''
                }
            }
        }

    }

    post {
        always {
            junit allowEmptyResults: true,
                  testResults: '**/target/*-reports/*.xml'
        }

        success {
            echo 'All MediBook services built, tested and pushed successfully!'
        }

        failure {
            echo 'MediBook CI/CD pipeline failed. Check the build logs.'
        }
    }
}
