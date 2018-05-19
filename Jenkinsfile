pipeline {
  agent any
  
  triggers {
    pollSCM('')
    githubPush()
  }
  
  options {
    buildDiscarder(logRotator(numToKeepStr: '1'))
    disableConcurrentBuilds()
    timeout(time: 1, unit: 'HOURS')
  }
  
  tools {
    jdk "jdk8"
    nodejs "node"
  }
  
  stages {
    stage("Build AS") {
      steps {
        withCredentials([string(credentialsId: 'gpg-pass-nannoq', variable: 'TOKEN')]) {
          script {
            sh "./gradlew install -Dsigning.gnupg.passphrase=$TOKEN -Dorg.gradle.parallel=false -Pcentral --info --stacktrace"
          }
        }
      }
    }
  }
  
  post {
    failure {
      mail to: 'mikkelsen.anders@gmail.com',
          subject: "Failed Pipeline: ${currentBuild.fullDisplayName}",
          body: "Something is wrong with ${env.BUILD_URL}"
    }
  }
}
