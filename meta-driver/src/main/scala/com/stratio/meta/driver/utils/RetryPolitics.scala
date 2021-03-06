/*
 * Stratio Meta
 *
 * Copyright (c) 2014, Stratio, All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */

package com.stratio.meta.driver.utils

import akka.actor.ActorRef
import com.stratio.meta.common.result.{ErrorType, ConnectResult, Result}
import scala.concurrent.Await
import akka.pattern.ask
import akka.util.Timeout

/**
 * Retry mechanism that send a message to an actor a number of times,
 * waiting between each retry.
 * @param retryTimes The number of times the query is retried.
 * @param waitTime Waiting time between retries.
 */
class RetryPolitics(retryTimes:Int,waitTime: Timeout) {
  def askRetry(remoteActor:ActorRef, message:AnyRef, waitTime:Timeout = this.waitTime , retry:Int = 0): Result={
    if(retry==retryTimes){
      Result.createConnectionErrorResult("Not found answer. After " + retry
                                         + " retries, timeout was exceed.");
    } else {
      try {
        val future = remoteActor.ask(message)(waitTime)
        Await.result(future.mapTo[Result], waitTime.duration*2)
      } catch {
        case ex: Exception => {
          println("Retry "+(retry+1)+" timeout")
          askRetry(remoteActor, message, waitTime, retry + 1)
        }
      }
    }
  }
}
