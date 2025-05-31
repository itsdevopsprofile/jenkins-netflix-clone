pipeline {
    agent any
    
    tools {
        jdk 'jdk17'
        nodejs 'node16'
        
    }
    environment {
        SCANNER_HOME = tool 'sonar-scanner'
    }
    
    stages{
        stage('code-checkout'){
            steps{
               git branch: 'main', changelog: false, poll: false, url: 'https://github.com/abhipraydhoble/netflix.git' 
            }
        }
        stage('sonar-analysis'){
            steps{
               withSonarQubeEnv('sonar-server') {
                sh '''$SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=Netflix \
                    -Dsonar.projectKey=sonar-token'''
                } 
            }
        }
        stage("quality gate") {
            steps {
                script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token'
                }
            }
        }
        stage('Install Dependencies') {
            steps {
                sh "npm install"
            }
        }
        stage('TRIVY FS SCAN') {
            steps {
                sh "trivy fs . > trivyfs.txt"
            }
        }
        stage("Docker Build & Push"){
            steps{
                script{
                   withDockerRegistry(credentialsId: 'docker', toolName: 'docker') {   
                       sh "docker build --build-arg TMDB_V3_API_KEY=079c53c7a0369363ae29016c9c3b29f6   -t netflix:v1 ."
                       sh "docker tag netflix abhipraydh96/netflix:v1 "
                       sh "docker push abhipraydh96/netflix:v1"
                    }
                }
            }
        }
        stage("TRIVY"){
            steps{
                sh "trivy image abhipraydh96/netflix:v1 > trivyimage.txt" 
            }
        }
        stage('Deploy to container'){
            steps{
                sh 'docker run -d --name movieflix -p 8082:80 abhipraydh96/netflix:v1'
                }
           }
       }
   }
