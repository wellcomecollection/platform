package uk.ac.wellcome.storage.dynamo

// This exception is thrown in the VersionedDao to indicate failures that
// might be retryable, for example:
//
//  - Hitting DynamoDB throughput limits -- wait a bit, then try again
//  - Having a version conflict with the existing record -- another process
//    was editing at the same time.  Read the updated record, then try
//    again if you still want to edit
//
// It wouldn't be thrown for errors that can't be recovered from,
// e.g. authentication errors.
//
case class DynamoNonFatalError(e: Throwable) extends Exception(e.getMessage)
