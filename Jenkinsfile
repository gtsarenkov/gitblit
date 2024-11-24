pipeline {
    agent any

    tools {
        // Configuring the JDK, assuming JDK 11 is installed and configured in Jenkins Tools
        jdk 'JDK21'
    }

    stages {
        stage('Output Environment') {
            steps {
                script {
                    if (isUnix()) {
                        sh 'printenv'
                    } else {
                        bat 'set'
                    }

                    // Print SCM configuration
                    if (isUnix()) {
                        sh '''
                        echo "Printing SCM Diagnostic Data..."
                        # Assuming git is used as SCM, adjust if needed
                        git config --list
                        echo "Current branch: $(git rev-parse --abbrev-ref HEAD)"
                        echo "Last commit: $(git log -1 --pretty=%B)"
                        '''
                    } else {
                        bat '''
                        echo Printing SCM Diagnostic Data...
                        REM Assuming git is used as SCM, adjust if needed
                        git config --list
                        echo Current branch:
                        git rev-parse --abbrev-ref HEAD
                        echo Last commit:
                        git log -1 --pretty=%%B
                        '''
                    }
                }
            }
        }

        stage('Checkout') {
            steps {
                // Checkout the code from the version control system
                checkout scm
            }
        }

        stage('Build') {
            steps {
                script {
                    if (isUnix()) {
                        sh './gradlew clean build'
                    } else {
                        bat 'gradlew.bat clean build'
                    }
                }
            }
        }
    }

    post {
        always {
            // Archive the build artifacts, assuming they are in the 'build/libs' directory
            archiveArtifacts artifacts: '**/build-gradled/libs/*.jar', allowEmptyArchive: true
            // Publish the test results, assuming they are in the 'build/test-results' directory
            junit '**/build-gradled/test-results/test/*.xml'
        }
    }
}