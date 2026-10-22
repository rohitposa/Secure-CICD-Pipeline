pipeline {
    agent any
    
    tools {
        maven 'maven3'
        jdk 'jdk21'
    }
    
    environment{
        SONAR_HOME = tool "sonar-scanner"
         IMAGE_TAG = "${BUILD_NUMBER}"
         DOCKER_USER = "rohitposa18"
    }
    
        stages {
            stage('checkout') {
                steps {
                    git branch: 'main', credentialsId: 'git_credential', url: 'https://github.com/rohitposa/shop-microservices.git'
                }
            }
            stage('compile'){
                steps {
                    sh "mvn compile"
                }
            }
            stage('test'){
                steps {
                    sh "mvn test"
                }
            }
            stage('scan filesystem') {
                steps {
                    sh "trivy fs --format table -o trivy-report.txt ."    
                }
            }
            stage('sonarqube analysis') {
                steps {
                    withSonarQubeEnv('sonar') {
                          sh '''$SONAR_HOME/bin/sonar-scanner -Dsonar.projectKey=shop-microservices -Dsonar.projectName=shop-microservices \
                              -Dsonar.exclusions=**/target/**,trivy-report.* \
                              -Dsonar.qualitygate.wait=true
                              '''
                    }   
                }
            }
            stage('build') {
                steps {
                    sh "mvn package"    
                }
            }
            stage('nexus artifact'){
                steps{
                    withMaven(globalMavenSettingsConfig: 'microservice', jdk: 'jdk21', maven: 'maven3', traceability: true) {
                        sh "mvn deploy"
                    }
                }
            }
            stage('docker build & tag'){
                steps{
                    script {
                            withDockerRegistry(credentialsId: 'docker-credential') {
                                  sh ''' 
                                  docker build -t ${DOCKER_USER}/product-service:${IMAGE_TAG} product-service
                                  docker build -t ${DOCKER_USER}/order-service:${IMAGE_TAG} order-service
                                  '''
                         }     
                    }
                }
            }
            stage('trivy image scan') {
                steps {
                    script {
                        sh '''
                         trivy image --format table -o product-report.txt ${DOCKER_USER}/product-service:${IMAGE_TAG}
                         trivy image --format table -o order-report.txt ${DOCKER_USER}/order-service:${IMAGE_TAG}
                         '''                
                    }
                }
            }
            stage('docker push'){
                steps{
                    script {
                            withDockerRegistry(credentialsId: 'docker-credential') {
                                  sh ''' 
                                   docker push ${DOCKER_USER}/product-service:${IMAGE_TAG}
                                   docker push ${DOCKER_USER}/order-service:${IMAGE_TAG}
                                   '''
                        }     
                    }
                }
            }
         stage('deploy with docker with rollback') {
        steps {
            sh '''
            PREV=$(cat /var/lib/jenkins/last_good_tag 2>/dev/null || true)
            docker compose pull
            docker compose up -d
            for i in $(seq 1 24); do
              curl -sf localhost:8081/actuator/health | grep -q UP && \
              curl -sf localhost:8082/actuator/health | grep -q UP && \
              echo ${IMAGE_TAG} > /var/lib/jenkins/last_good_tag && exit 0
              sleep 5
            done
            echo "Health check failed, rolling back to $PREV"
            [ -n "$PREV" ] && IMAGE_TAG=$PREV docker compose up -d
            exit 1
            '''
            }
        }
    }
    post {
    always {
        sh 'docker image prune -f'
    }
    }
}
