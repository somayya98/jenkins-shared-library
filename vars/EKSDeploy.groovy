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
            appVersion = configMap.get("appVersion")
            ACC_ID = "930832106480"
            PROJECT = configMap.get("project")
            COMPONENT = configMap.get("component")
            deploy_to = configMap.get("deploy_to")
            REGION = "us-east-1"
        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        // parameters {
        //     string(name: 'appVersion', description: 'Which app version you want to deploy')
        //     choice(name: 'deploy_to', choices: ['dev', 'qa', 'produ'], description: 'Pick something') 
        // }
        // This is build section
        stages {
            
            stage('Deploy') {
                steps {
                    script{
                        withAWS(region:'us-east-1',credentials:'aws-creds') {
                            sh """
                                set -e
                                aws eks update-kubeconfig --region ${REGION} --name ${PROJECT}-${deploy_to}
                                kubectl get nodes
                                sed -i "s/IMAGE_VERSION/${appVersion}/g" values.yaml
                                helm upgrade --install ${COMPONENT} -f values-${deploy_to}.yaml -n ${PROJECT} --atomic --wait --timeout=5m .
                            """
                        }
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
