pipeline {
    agent any
    tools {
        maven 'Maven 3'
        jdk 'Java 8'
    }
    options {
        buildDiscarder(logRotator(artifactNumToKeepStr: '5'))
    }
    stages {
        stage ('Build') {
            steps {
                sh 'mvn clean package'
            }
            post {
                success {
                    archiveArtifacts artifacts: '**/target/*.jar', excludes: '**/target/floodgate-*-*.jar', fingerprint: true
                }
            }
        }

        stage ('Deploy') {
            when {
                anyOf {
                    branch "master"
                    branch "development"
                }
            }
            steps {
                sh 'mvn javadoc:jar source:jar deploy -DskipTests'
            }
        }
    }

    post {
        always {
            deleteDir()
            withCredentials([string(credentialsId: 'geyser-discord-webhook', variable: 'DISCORD_WEBHOOK')]) {
                discordSend description: "**Build:** [${currentBuild.id}](${env.BUILD_URL})\n**Status:** [${currentBuild.currentResult}](${env.BUILD_URL})\n\n[**Artifacts on Jenkins**](https://ci.nukkitx.com/job/GeyserMC/job/Floodgate)", footer: 'NukkitX Jenkins', link: env.BUILD_URL, successful: currentBuild.resultIsBetterOrEqualTo('SUCCESS'), title: "${env.JOB_NAME} #${currentBuild.id}", webhookURL: DISCORD_WEBHOOK
            }
        }
    }
}
