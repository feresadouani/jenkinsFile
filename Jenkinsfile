pipeline {
    agent any

    environment {
        DOCKERHUB_REPO = "feresadouani/pipeline"
        IMAGE_TAG = "latest"
    }

    stages {


        stage('Build Maven') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
  stage('MVN SONARQUBE') {
             steps {
                 withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                     sh """
                         mvn sonar:sonar \
                           -Dsonar.login=$SONAR_TOKEN \
                           -Dsonar.host.url=http://localhost:9000
                     """
                 }
             }
         }
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        docker build -t $DOCKERHUB_REPO:$IMAGE_TAG .
                    """
                }
            }
        }

        stage('Login to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USER', passwordVariable: 'PASS')]) {
                        sh "echo $PASS | docker login -u $USER --password-stdin"
                    }
                }
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    sh "docker push $DOCKERHUB_REPO:$IMAGE_TAG"
                }
            }
        }
    }
}

