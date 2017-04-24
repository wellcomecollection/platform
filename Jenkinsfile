stage("Checkout") {
  node {
    git 'https://github.com/wellcometrust/platform-api'
    stash name: 'sources', excludes: 'target'
  }
}

node {
  def stages = ["compile", "test"]
  def projects = ["common", "api", "calm_adapter", "transformer", "ingestor"]

  stage("Test") {
    def stepsForParallel = [:]
    for (projectName in projects) {
      def projectName = projects.get(i)
      def compileStep = "${projectName}"
      stepsForParallel[compileStep] = createBuildStep(projectName, "test")
    }
    parallel stepsForParallel
  }

  if (env.BRANCH_NAME == "master") {
    stage("Deploy") {
      def stepsForParallel = [:]
      for (projectName in projects) {
        /* The common lib doesn't have a buildable container */
        if (projectName == "common") {
          continue
        }

        def compileStep = "${projectName}"
        stepsForParallel[compileStep] = createBuildStep(projectName, "deploy")
      }
      parallel stepsForParallel
    }
  }
}

def createBuildStep(String project, String action) {
  return {
    node {
      unstash 'sources'
      sh "/usr/bin/sbt ${project}/${action}"
    }
  }
}
