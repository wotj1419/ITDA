// ================================
// Mattermost 알림 함수
// ================================
def sendMMNotify(boolean success, Map info) {
    try {
        def titleLine = success ? "## :jenkins7: 배포 성공 ✅"
                                : "## :angry_jenkins: 배포 실패 ❌"

        def lines = []
        if (info.mention) lines << "**작성자**: ${info.mention}"
        if (info.branch)  lines << "**브랜치**: ${info.branch}"

        if (info.commit?.msg) {
            def commitLine = info.commit?.url ?
                "[${info.commit.msg}](${info.commit.url})" :
                info.commit.msg
            lines << "**커밋**: ${commitLine}"
        }

        if (info.buildUrl) lines << "**Jenkins**: [빌드 보기](${info.buildUrl})"

        if (!success && info.details) {
            lines << "**에러 로그**:\n```\n${info.details}\n```"
        }

        def text = "${titleLine}\n" + (lines ? ("\n" + lines.join("\n")) : "")

        writeFile file: 'payload.json', text: groovy.json.JsonOutput.toJson([
            text      : text,
            username  : "Jenkins",
            icon_emoji: ":jenkins7:"
        ])

        withCredentials([string(credentialsId: 'mattermost-webhook', variable: 'MM_WEBHOOK')]) {
            sh """
            curl -sS -X POST -H 'Content-Type: application/json' \
            --data-binary @payload.json \
            "$MM_WEBHOOK" || true
            """
        }
    } catch (err) {
        echo "Mattermost notify failed: ${err}"
    }
}

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
                    env.REPO_URL = scmVars.GIT_URL ?: env.GIT_URL
                    env.COMMIT_SHA = scmVars.GIT_COMMIT ?: env.GIT_COMMIT
                    env.COMMIT_MSG = sh(script: "git log -1 --pretty=%s", returnStdout: true).trim()
                    env.COMMIT_URL = env.REPO_URL ? "${env.REPO_URL.replace('.git','')}/commit/${env.COMMIT_SHA}" : ''
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

    post {
        success {
            script {
                def branch = env.DEPLOY_BRANCH ?: env.GIT_BRANCH ?: env.GIT_LOCAL_BRANCH ?: env.gitlabBranch ?: env.gitlabSourceBranch
                sendMMNotify(true, [
                    mention : "@here",
                    branch  : branch,
                    commit  : [
                        msg: env.COMMIT_MSG,
                        url: env.COMMIT_URL
                    ],
                    buildUrl: env.BUILD_URL
                ])
            }
        }

        failure {
            script {
                def branch = env.DEPLOY_BRANCH ?: env.GIT_BRANCH ?: env.GIT_LOCAL_BRANCH ?: env.gitlabBranch ?: env.gitlabSourceBranch
                def details = ''
                try {
                    details = sh(script: "docker ps -a | tail -n 10", returnStdout: true).trim()
                } catch (err) {
                    details = "에러 로그 수집 실패: ${err}"
                }
                sendMMNotify(false, [
                    mention : "@here",
                    branch  : branch,
                    buildUrl: env.BUILD_URL,
                    details : details
                ])
            }
        }
    }
}
