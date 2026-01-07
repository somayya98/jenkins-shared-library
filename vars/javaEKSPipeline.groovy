def call (Map configMap){
    pipeline {
    // These are pre-build sections
        agent {
            node {
                label 'AGENT-1'
            }
        }
        environment {
            COURSE = "Jenkins"
            appVersion = ""
            ACC_ID = "930832106480"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
        }
        options {
            timeout(time: 60, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        // This is build section
        stages {
            stage('Read Version') {
                steps {
                    script{
                        def pom = readMavenPom file: 'pom.xml'
                        appVersion = pom.version
                        echo "app version: ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    script{
                        sh """
                            mvn clean package
                        """
                    }
                }
            }
            stage('Unit Test') {
                steps {
                    script{
                        sh """
                            echo test
                        """
                    }
                }
            }

            stage('Build Image') {
                steps {
                    script{
                        withAWS(region:'us-east-1',credentials:'aws-creds') {
                            sh """
                                aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                                docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                                docker images
                                docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                            """
                        }
                    }
                }
            }

            // stage('Trivy Scan'){
            //     steps {
            //         script{
            //             sh """
            //                 trivy image \
            //                 --scanners vuln \
            //                 --severity HIGH,CRITICAL,MEDIUM \
            //                 --pkg-types os \
            //                 --exit-code 1 \
            //                 --format table \
            //                 ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
            //             """
            //         }
            //     }
            // }
            stage('Trigger DEV Deploy') {
                steps {
                    script {
                        build job: "../${COMPONENT}-deploy",
                            wait: false, // Wait for completion
                            propagate: false, // Propagate status
                            parameters: [
                                string(name: 'appVersion', value: "${appVersion}"),
                                string(name: 'deploy_to', value: "dev")
                            ]
                    }
                }
            }

        }   

        post {
            always {
                echo 'Pipeline finished'
                cleanWs()
            }
            success {
                echo 'Pipeline succeeded'
            }
            failure {
                echo 'Pipeline failed'
            }
            aborted {
                echo 'Pipeline aborted'
            }
        }
    }
}

