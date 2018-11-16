 package uk.ac.wellcome

 import akka.Done
 import com.typesafe.config.{Config, ConfigFactory}
 import grizzled.slf4j.Logging

 import scala.concurrent.duration.Duration
 import scala.concurrent.{Await, Future}

 trait WorkerService {
   def run(): Future[Done]
 }

 trait WellcomeApp extends App with Logging {
   def buildWorkerService(config: Config): WorkerService

   def run() = {
     val config: Config = ConfigFactory.load()

     val workerService = buildWorkerService(config)

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
