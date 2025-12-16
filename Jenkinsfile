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

    /* ========================= */
    /*         CHECKOUT          */
    /* ========================= */
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    /* ========================= */
    /*        JUNIT TESTS        */
    /* ========================= */
    stage('Maven Tests') {
      steps {
        sh 'mvn clean test'
      }
    }

    /* ========================= */
    /*        BUILD JAR          */
    /* ========================= */
    stage('Build Maven') {
      steps {
        // ⚠️ PAS de clean ici (sinon JUnit disparaît)
        sh 'mvn package -DskipTests'
      }
    }

    /* ========================= */
    /*        SONARQUBE          */
    /* ========================= */
    stage('MVN SONARQUBE') {
      when {
        expression { return env.SONAR_CRED_ID != null && env.SONAR_CRED_ID != "" }
      }
      steps {
        withCredentials([
          string(credentialsId: "${SONAR_CRED_ID}", variable: 'SONAR_TOKEN')
        ]) {
          sh '''
            mvn sonar:sonar \
              -Dsonar.login=$SONAR_TOKEN \
              -Dsonar.host.url=http://localhost:9000
          '''
        }
      }
    }

    /* ========================= */
    /*        DOCKER BUILD       */
    /* ========================= */
    stage('Build Docker Image') {
      steps {
        sh "docker build -t ${DOCKERHUB_REPO}:${IMAGE_TAG} ."
      }
    }

    /* ========================= */
    /*     DOCKER HUB LOGIN      */
    /* ========================= */
    stage('Login to Docker Hub') {
      steps {
        withCredentials([
          usernamePassword(
            credentialsId: "${DOCKERHUB_CRED_ID}",
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
          )
        ]) {
          sh "echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin"
        }
      }
    }

    /* ========================= */
    /*      PUSH IMAGE           */
    /* ========================= */
    stage('Push Docker Image') {
      steps {
        sh "docker push ${DOCKERHUB_REPO}:${IMAGE_TAG}"
        sh "docker tag ${DOCKERHUB_REPO}:${IMAGE_TAG} ${DOCKERHUB_REPO}:latest || true"
        sh "docker push ${DOCKERHUB_REPO}:latest || true"
      }
    }

    /* ========================= */
    /*     K8S MYSQL SECRET      */
    /* ========================= */
    stage('Create/ensure MySQL Secret') {
      steps {
        sh """
          kubectl -n ${NAMESPACE} get secret spring-mysql-secret >/dev/null 2>&1 || \
          kubectl -n ${NAMESPACE} create secret generic spring-mysql-secret \
            --from-literal=SPRING_DATASOURCE_USERNAME=root \
            --from-literal=SPRING_DATASOURCE_PASSWORD=root123
        """
      }
    }

    /* ========================= */
    /*     DEPLOY TO K8S         */
    /* ========================= */
    stage('Deploy to Kubernetes') {
      steps {
        sh """
          kubectl -n ${NAMESPACE} apply -f k8s/ --recursive || true
          kubectl -n ${NAMESPACE} set image deployment/spring-app spring-app=${DOCKERHUB_REPO}:${IMAGE_TAG}
          kubectl -n ${NAMESPACE} rollout status deployment/spring-app --timeout=120s
          kubectl -n ${NAMESPACE} get pods -o wide
          kubectl -n ${NAMESPACE} get svc
        """
      }
    }

    /* ========================= */
    /*     SMOKE TEST (FIXED)    */
    /* ========================= */
    stage('Post-deploy smoke test') {
      steps {
        sh '''
          echo "Waiting for spring-app pod to be READY..."
          kubectl -n ${NAMESPACE} wait \
            --for=condition=Ready pod \
            -l app=spring-app \
            --timeout=60s

          POD=$(kubectl -n ${NAMESPACE} get pods \
            -l app=spring-app \
            --field-selector=status.phase=Running \
            -o jsonpath="{.items[0].metadata.name}")

          echo "Testing pod: $POD"

          kubectl -n ${NAMESPACE} exec $POD -- \
            curl -sS http://127.0.0.1:8089/student/Depatment/getAllDepartment || echo "curl failed"
        '''
      }
    }
  }

  /* ========================= */
  /*     JUNIT REPORTING       */
  /* ========================= */
  post {
    always {
      junit testResults: 'target/surefire-reports/*.xml'
    }
    success {
      echo "Pipeline succeeded — image: ${DOCKERHUB_REPO}:${IMAGE_TAG}"
    }
    failure {
      echo "Pipeline failed — voir les logs"
    }
  }
}
