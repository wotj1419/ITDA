pipeline {
    agent any
    tools {
        gradle 'S14P_gradle'
        nodejs 'Node-22'
    }
    environment {
        DOCKER_IMAGE = 'yh2222/aimovie-api:latest'
        DEPLOY_DIR = '/opt/itda/deploy'
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

        stage('Docker Build') {
            steps {
                dir('itda-backend') {
                    sh 'docker build -t $DOCKER_IMAGE .'
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

        stage('Deploy') {
            steps {
                sh '''
                    set -e
                    mkdir -p "$DEPLOY_DIR/nginx/conf.d" "$DEPLOY_DIR/prometheus" "$DEPLOY_DIR/grafana/provisioning"
                    mkdir -p "$DEPLOY_DIR/nginx/html"
                    cp deploy/docker-compose.yml "$DEPLOY_DIR/"
                    cp deploy/nginx/conf.d/app.conf "$DEPLOY_DIR/nginx/conf.d/"
                    cp deploy/prometheus/prometheus.yml "$DEPLOY_DIR/prometheus/" || true
                    cp -r itda-frontend/dist/. "$DEPLOY_DIR/nginx/html/"
                    if [ ! -f "$DEPLOY_DIR/.env" ]; then
                      if [ -f deploy/.env ]; then
                        cp deploy/.env "$DEPLOY_DIR/.env"
                      else
                        cp deploy/.env.example "$DEPLOY_DIR/.env"
                      fi
                    fi
                    sed -i "s|^API_IMAGE=.*|API_IMAGE=$DOCKER_IMAGE|" "$DEPLOY_DIR/.env"
                    docker compose -f "$DEPLOY_DIR/docker-compose.yml" up -d nginx api mysql redis
                '''
            }
        }
    }
}
