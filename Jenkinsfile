// Mattermost 알림 함수
def sendMMNotify(boolean success, Map info) {
    try {
        def action = info.action ?: "Build"
        def titleLine = success ? "## :jenkins7: ${action} 성공 ✅"
                                : "## :angry_jenkins: ${action} 실패 ❌"

        def lines = []
        def mergeTarget = info.mergeTarget?.toString()?.trim()
        if (mergeTarget && mergeTarget != "null") {
            def mergeSource = info.mergeSource ?: info.branch ?: "unknown"
            def mergeSourceText = mergeSource?.toString()?.trim()
            if (!mergeSourceText || mergeSourceText == "null") {
                mergeSourceText = "unknown"
            }
            lines << "**머지**: ${mergeSourceText} -> ${mergeTarget}"
        }
        if (info.mention) lines << "**알림**: ${info.mention}"
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
            sh '''
              set +x
              curl -sS -H 'Content-Type: application/json' \
                --data-binary @payload.json \
                "$MM_WEBHOOK" || true
            '''
        }
    } catch (err) {
        echo "Mattermost notify failed: ${err}"
    }
}

def shLog(String cmd) {
    sh(script: """#!/bin/bash
set -e
set -o pipefail
( ${cmd} ) 2>&1 | tee -a "${env.WORKSPACE}/${env.LOG_FILE}"
""")
}

def normalizeBranchName(String raw) {
    if (!raw) {
        return "unknown"
    }
    return raw.replaceFirst(/^origin\//, "").replaceFirst(/^refs\/heads\//, "")
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
        LOG_FILE = 'jenkins-console.log'
    }

    stages {
        stage('Init') {
            steps {
                sh 'rm -f "$WORKSPACE/$LOG_FILE"; touch "$WORKSPACE/$LOG_FILE"'
                script {
                    def rawBranch = env.BRANCH_NAME ?: env.CHANGE_BRANCH ?: env.GIT_LOCAL_BRANCH ?: env.GIT_BRANCH
                    env.BUILD_BRANCH = normalizeBranchName(rawBranch)
                    env.MERGE_TARGET = env.CHANGE_TARGET
                    env.MERGE_SOURCE = env.CHANGE_BRANCH
                    env.REPO_URL = env.GIT_URL
                    env.COMMIT_SHA = env.GIT_COMMIT
                    env.COMMIT_MSG = sh(script: "git log -1 --pretty=%s", returnStdout: true).trim()
                    env.COMMIT_URL = env.REPO_URL ? "${env.REPO_URL.replace('.git','')}/commit/${env.COMMIT_SHA}" : ''
                    echo "브랜치정보 BRANCH_NAME=${env.BRANCH_NAME} GIT_BRANCH=${env.GIT_BRANCH} GIT_LOCAL_BRANCH=${env.GIT_LOCAL_BRANCH} GIT_URL=${env.GIT_URL}"
                }
            }
        }

        stage('Backend Build') {
            steps {
                dir('itda-backend') {
                    shLog 'gradle clean assemble -x test'
                }
            }
        }

        stage('Docker Build') {
            steps {
                dir('itda-backend') {
                    shLog 'docker build -t $DOCKER_IMAGE .'
                }
            }
        }

        stage('Frontend Install') {
            steps {
                dir('itda-frontend') {
                    shLog 'npm ci'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir('itda-frontend') {
                    shLog 'npm run build'
                }
            }
        }

        stage('Test') {
            when {
                branch 'test/*'
            }
            steps {
                script { env.DID_TEST = 'true' }
                dir('itda-backend') {
                    shLog 'gradle test'
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'develop'
            }
            steps {
                script { env.DID_DEPLOY = 'true' }
                shLog '''
                    set -e
                    mkdir -p "$DEPLOY_DIR/nginx/conf.d" "$DEPLOY_DIR/prometheus" "$DEPLOY_DIR/grafana/provisioning"
                    mkdir -p "$DEPLOY_DIR/nginx/html"
                    mkdir -p "$DEPLOY_DIR/db"
                    cp deploy/docker-compose.yml "$DEPLOY_DIR/"
                    cp deploy/nginx/conf.d/app.conf "$DEPLOY_DIR/nginx/conf.d/"
                    cp deploy/prometheus/prometheus.yml "$DEPLOY_DIR/prometheus/" || true
                    cp -r itda-frontend/dist/. "$DEPLOY_DIR/nginx/html/"
                    cp itda-backend/src/main/resources/sql/schema-local.sql "$DEPLOY_DIR/db/"
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
                def action = env.DID_DEPLOY == 'true' ? 'Deploy' : (env.DID_TEST == 'true' ? 'Test' : 'Build')
                sendMMNotify(true, [
                    mention : "@here",
                    branch  : env.BUILD_BRANCH,
                    mergeTarget : env.MERGE_TARGET,
                    mergeSource : env.MERGE_SOURCE,
                    action  : action,
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
                def action = env.DID_DEPLOY == 'true' ? 'Deploy' : (env.DID_TEST == 'true' ? 'Test' : 'Build')
                def details = ''
                try {
                    details = sh(script: 'if [ -f "$WORKSPACE/$LOG_FILE" ]; then tail -n 200 "$WORKSPACE/$LOG_FILE"; else echo "No log file available."; fi', returnStdout: true).trim()
                    if (details.length() > 4000) { details = details.substring(details.length() - 4000) }
                } catch (err) {
                    details = "에러 로그 수집 실패: ${err}"
                }
                sendMMNotify(false, [
                    mention : "@here",
                    branch  : env.BUILD_BRANCH,
                    mergeTarget : env.MERGE_TARGET,
                    mergeSource : env.MERGE_SOURCE,
                    action  : action,
                    details : details
                ])
            }
        }
    }
}

