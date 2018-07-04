pipeline {
  agent any

  stages {
    stage('Build') {
      steps {
        echo 'Building..'
      }
    }
    stage('Test') {
      parallel
        ingestor: {
          node('ingestor') {
            checkout scm
            echo 'Testing ingestor'
          }
        }
        id_minter: {
          node('id_minter') {
            checkout scm
            echo 'Testing id_minter'
          }
        }
    }
    stage('Deploy') {
      steps {
        echo 'Deploying....'
      }
    }
  }
}
