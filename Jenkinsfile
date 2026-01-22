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
                script {
                    def scmVars = checkout scm
                    // Persist branch info for single-pipeline jobs where BRANCH_NAME is empty.
                    env.DEPLOY_BRANCH = scmVars.GIT_BRANCH
                    echo "DEPLOY_BRANCH=${env.DEPLOY_BRANCH} GIT_BRANCH=${env.GIT_BRANCH} GIT_LOCAL_BRANCH=${env.GIT_LOCAL_BRANCH} gitlabBranch=${env.gitlabBranch} gitlabSourceBranch=${env.gitlabSourceBranch} gitlabTargetBranch=${env.gitlabTargetBranch}"
                }
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
            when {
                expression {
                    def b = env.DEPLOY_BRANCH ?: env.GIT_BRANCH ?: env.GIT_LOCAL_BRANCH ?: env.gitlabBranch ?: env.gitlabSourceBranch ?: ''
                    return b == 'develop' || b.endsWith('/develop') || b == 'refs/remotes/origin/develop'
                }
            }
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
