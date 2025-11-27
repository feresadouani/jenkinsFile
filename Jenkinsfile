pipeline {
    agent any

    stages {
        stage('Hello') {
            steps {
                echo "Hello depuis Jenkinsfile !"
            }
        }

        stage('Build Maven') {
            steps {
                sh 'mvn clean package'
            }
        }
    }
}


