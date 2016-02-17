package com.flipkart.utils.http

import java.util.concurrent.{Semaphore, TimeUnit}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.flipkart.utils.http.HttpClient.Header
import com.flipkart.utils.http.metrics.MetricRegistry
import com.sun.istack.internal.NotNull
import org.apache.commons.io.IOUtils
import org.apache.http.HttpResponse
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods._
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager

import scala.reflect.{ClassTag, _}
import scala.util.{Failure, Success, Try}

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */

class HttpClient(name: String, @NotNull ttlInMillis: Long, @NotNull maxConnections: Int, @NotNull processQueueSize: Int, @NotNull connectionTimeoutInMillis: Integer, @NotNull socketTimeoutInMillis: Integer) {

  protected var headers: List[Header] = List()
  protected val processQueue = new Semaphore(processQueueSize + maxConnections)

  val connectionManager = new PoolingHttpClientConnectionManager(ttlInMillis, TimeUnit.MILLISECONDS)
  connectionManager.setMaxTotal(maxConnections)
  connectionManager.setDefaultMaxPerRoute(maxConnections)

  val httpParams = RequestConfig.custom().setConnectTimeout(connectionTimeoutInMillis).setSocketTimeout(socketTimeoutInMillis).build()

  protected val apacheHttpClient = HttpClientBuilder.create()
    .setConnectionManager(connectionManager).setDefaultRequestConfig(httpParams).build()

  protected def metrics(suffix: String) = MetricRegistry.registry.timer(com.codahale.metrics.MetricRegistry.name(getClass, s"$name.$suffix"))

  def setDefaultHeaders(headers: Iterable[Header]): HttpClient = {
    this.headers = headers.toList
    this
  }

  def doGet(url: String, headers: Iterable[Header] = List()): Try[HttpResponse] = {
    val httpGet = new HttpGet(url)
    headers.foreach(h => httpGet.addHeader(h._1, h._2))
    doExecute(httpGet)
  }

  def doDelete(url: String, headers: Iterable[Header] = List()): Try[HttpResponse] = {
    val httpDelete = new HttpDelete(url)
    headers.foreach(h => httpDelete.addHeader(h._1, h._2))
    doExecute(httpDelete)
  }

  def doPost(url: String, body: Array[Byte], headers: Iterable[Header] = List()): Try[HttpResponse] = {
    val httpPost = new HttpPost(url)
    httpPost.setEntity(new ByteArrayEntity(body))
    headers.foreach(h => httpPost.addHeader(h._1, h._2))
    doExecute(httpPost)
  }

  def doPut(url: String, body: Array[Byte], headers: Iterable[Header] = List()): Try[HttpResponse] = {
    val httpPut = new HttpPut(url)
    httpPut.setEntity(new ByteArrayEntity(body))
    headers.foreach(h => httpPut.addHeader(h._1, h._2))
    doExecute(httpPut)
  }

  def doExecute(request: HttpRequestBase): Try[HttpResponse] = {
    this.headers.foreach(h => request.addHeader(h._1, h._2))
    if (processQueue.tryAcquire()) {
      val timed = metrics(request.getMethod).time()
      try {
        Success(apacheHttpClient.execute(request))
      } catch {
        case e: Throwable =>
          Failure(e)
      } finally {
        timed.stop()
        processQueue.release()
      }
    } else {
      Failure(new Exception("PROCESS_QUEUE_FULL"))
    }
  }


}


object HttpClient {

  type Header = (String, String)

  val objMapper = new ObjectMapper() with ScalaObjectMapper
  objMapper.registerModules(Seq(DefaultScalaModule): _*)

  implicit class UnMarshallFunctions(val response: HttpResponse) {
    def getObj[T: ClassTag] = {
      objMapper.readValue(response.getEntity.getContent, classTag[T].runtimeClass).asInstanceOf[T]
    }

    def getString(encoding: String = "UTF-8") = {
      IOUtils.toString(response.getEntity.getContent, encoding)
    }

  }

}