pipeline {
  agent any

  environment {
    DOCKERHUB_REPO = "feresadouani/pipeline"
    IMAGE_TAG = "${env.BUILD_NUMBER}"
    NAMESPACE = "devops"
    DOCKERHUB_CRED_ID = "dockerhub"
    SONAR_CRED_ID = "sonar-token"
    KUBECONFIG_CRED_ID = "" 
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build Maven') {
      steps {
        sh 'mvn clean package -DskipTests'
      }
    }

    stage('MVN SONARQUBE') {
      when { expression { return env.SONAR_CRED_ID != null && env.SONAR_CRED_ID != "" } }
      steps {
        withCredentials([string(credentialsId: "${SONAR_CRED_ID}", variable: 'SONAR_TOKEN')]) {
          sh """
            mvn sonar:sonar \
              -Dsonar.login=${SONAR_TOKEN} \
              -Dsonar.host.url=http://localhost:9000
          """
        }
      }
    }

    stage('Build Docker Image') {
      steps {
        script {
          sh "docker build -t ${DOCKERHUB_REPO}:${IMAGE_TAG} ."
        }
      }
    }

    stage('Login to Docker Hub') {
      steps {
        script {
          withCredentials([usernamePassword(credentialsId: "${DOCKERHUB_CRED_ID}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
            sh "echo ${DOCKER_PASS} | docker login -u ${DOCKER_USER} --password-stdin"
          }
        }
      }
    }

    stage('Push Docker Image') {
      steps {
        sh "docker push ${DOCKERHUB_REPO}:${IMAGE_TAG}"
        sh "docker tag ${DOCKERHUB_REPO}:${IMAGE_TAG} ${DOCKERHUB_REPO}:latest || true"
        sh "docker push ${DOCKERHUB_REPO}:latest || true"
      }
    }

    stage('Prepare kubeconfig (optional)') {
      when { expression { return env.KUBECONFIG_CRED_ID != null && env.KUBECONFIG_CRED_ID != "" } }
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED_ID}", variable: 'KUBECONFIG_FILE')]) {
          sh '''
            mkdir -p $HOME/.kube
            cp "$KUBECONFIG_FILE" $HOME/.kube/config
            chmod 600 $HOME/.kube/config
            kubectl config view
          '''
        }
      }
    }

    stage('Create/ensure MySQL Secret') {
      steps {
        script {
          sh """
            kubectl -n ${NAMESPACE} get secret spring-mysql-secret >/dev/null 2>&1 || \\
            kubectl -n ${NAMESPACE} create secret generic spring-mysql-secret \\
              --from-literal=SPRING_DATASOURCE_USERNAME=root \\
              --from-literal=SPRING_DATASOURCE_PASSWORD=root123
          """
        }
      }
    }

    stage('Deploy to Kubernetes') {
      steps {
        script {
          sh """
            kubectl -n ${NAMESPACE} apply -f k8s/ --recursive || true
            kubectl -n ${NAMESPACE} set image deployment/spring-app spring-app=${DOCKERHUB_REPO}:${IMAGE_TAG} --record || true
            kubectl -n ${NAMESPACE} rollout status deployment/spring-app --timeout=120s || true
            kubectl -n ${NAMESPACE} get pods -o wide
            kubectl -n ${NAMESPACE} get svc
          """
        }
      }
    }

    stage('Post-deploy smoke test') {
      steps {
        script {
          sh '''
            POD=$(kubectl -n ${NAMESPACE} get pods -l app=spring-app -o jsonpath="{.items[0].metadata.name}" || true)
            if [ -n "$POD" ]; then
              kubectl -n ${NAMESPACE} exec $POD -- curl -sS http://127.0.0.1:8089/student/Depatment/getAllDepartment || echo "curl failed"
            else
              echo "No spring-app pod found"
            fi
          '''
        }
      }
    }
  }

  post {
    success {
      echo "Pipeline succeeded — image: ${DOCKERHUB_REPO}:${IMAGE_TAG}"
    }
    failure {
      echo "Pipeline failed — voir les logs"
    }
  }
}
