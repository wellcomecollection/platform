package uk.ac.wellcome.platform.archive.notifier.flows.callback

import akka.stream.scaladsl.Flow
import uk.ac.wellcome.platform.archive.common.progress.models.Progress
import uk.ac.wellcome.platform.archive.notifier.models.CallbackFlowResult

object WithoutCallbackUrlFlow {
  // This flow handles the case where there isn't a callback URL on
  // the progress object -- there's nothing to do, just prepare the result.
  //
  def apply() = Flow[Progress]
    .filter { _.callbackUrl.isEmpty }
    .map { progress: Progress =>
      CallbackFlowResult(progress = progress, httpResponse = None)
    }

}
