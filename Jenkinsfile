stage("Checkout") {
  node {
    git 'https://github.com/wellcometrust/platform-api'
    stash name: 'sources', excludes: 'target'
  }
}

node {
  stage("Compile") {
    def projects = ["common", "api", "calm_adapter", "transformer", "ingestor"]
    def stepsForParallel = [:]

    for (int i = 0; i < projects.size(); i++) {
      def s = projects.get(i)
      def compileStep = "${s}"
      stepsForParallel[compileStep] = transformIntoCompileStep(s)
    }

    parallel stepsForParallel
  }
}

def transformIntoCompileStep(String project) {
  return {
    node {
      unstash 'sources'
      sh "/usr/bin/sbt ${project}/compile"
    }
  }
}
