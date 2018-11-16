 package uk.ac.wellcome

 import akka.Done
 import grizzled.slf4j.Logging

 import scala.concurrent.duration.Duration
 import scala.concurrent.{Await, Future}

 trait WorkerService {
   def run(): Future[Done]
 }

 trait WellcomeApp extends App with Logging {
   def run(workerService: WorkerService) = {
     try {
       info("Starting worker.")

       val result = workerService.run()

       Await.result(result, Duration.Inf)
     } catch {
       case e: Throwable =>
         error("Fatal error:", e)
     } finally {
       info("Terminating worker.")
     }
   }
 }
