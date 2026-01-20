pipeline {
    agent any
    tools {
        gradle 'S14P_gradle'
        nodejs 'Node-22'
    }
    options {
        skipDefaultCheckout()
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                dir('itda-backend') {
                    sh 'gradle clean assemble -x test'
                }
            }
        }

        stage('Frontend Install') {
            steps {
                dir('itda-frontend') {
                    sh 'npm ci'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('itda-frontend') {
                    sh 'npm run build'
                }
            }
        }
    }
}
