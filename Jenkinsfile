pipeline {
  agent any

  environment {
    DOCKERHUB_REPO = "feresadouani/pipeline"   // ton repo DockerHub
    IMAGE_TAG = "${env.BUILD_NUMBER}"          // tag par build (meilleur que "latest")
    NAMESPACE = "devops"
    DOCKERHUB_CRED_ID = "dockerhub"           // credential id (username/password)
    SONAR_CRED_ID = "sonar-token"             // token pour Sonar (string)
    # Option: si tu as sauvegardé un kubeconfig en tant que Jenkins "Secret file", 
    # mets son id ici et décommente la partie Prepare kubeconfig.
    KUBECONFIG_CRED_ID = ""                   // e.g. "kubeconfig" (laisser vide si kubeconfig est déjà sur l'agent)
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
        // Optionnel : mettre à jour le tag latest
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
            echo "kubeconfig prepared:"
            kubectl config view
          '''
        }
      }
    }

    stage('Create/ensure MySQL Secret') {
      steps {
        script {
          // crée le secret only-if-not-exists (non destructif)
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
          // si ton dossier k8s/ contient manifests, on applique d'abord (crée deployment/service si manquant)
          sh """
            # apply any manifests (idempotent)
            kubectl -n ${NAMESPACE} apply -f k8s/ --recursive || true

            # patch/set the image to the newly pushed image
            kubectl -n ${NAMESPACE} set image deployment/spring-app spring-app=${DOCKERHUB_REPO}:${IMAGE_TAG} --record || true

            # wait for rollout
            kubectl -n ${NAMESPACE} rollout status deployment/spring-app --timeout=120s || true

            # debug info
            kubectl -n ${NAMESPACE} get pods -o wide
            kubectl -n ${NAMESPACE} get svc
          """
        }
      }
    }

    stage('Post-deploy smoke test') {
      steps {
        script {
          // tentative d'appel d'un endpoint health (ne bloque pas le pipeline si curl échoue)
          sh '''
            echo "Trying to curl /student/Depatment/getAllDepartment ..."
            POD=$(kubectl -n ${NAMESPACE} get pods -l app=spring-app -o jsonpath="{.items[0].metadata.name}")
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
