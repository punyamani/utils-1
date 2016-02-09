package com.flipkart.utils.config

import java.io.IOException
import java.net.UnknownHostException

import com.flipkart.kloud.config.error.ConfigServiceException
import com.flipkart.kloud.config.{Bucket, BucketUpdateListener, ConfigClient}
import com.flipkart.utils.NetworkUtils
import com.flipkart.utils.config.KloudConfig.BucketIds
import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.LoggerFactory

import scala.collection.mutable


class KloudConfig(configHost: String, configPort: Int)
                 (bucketIds: BucketIds) extends ConfigAccumulator with IKloudConfig {

  val logger = LoggerFactory.getLogger(classOf[KloudConfig])

  var cfgClient: ConfigClient = null

  /** BucketId -> Config */
  val bucketConfigs = mutable.LinkedHashMap[String, Config]()

  private def readConfigs(): List[Config] = {
    cfgClient = new ConfigClient(configHost, configPort, 1, 30000)
    logger.info(s"Buckets to fetch config: [${bucketIds.toList}]")

    bucketIds.foreach(bucketId => {

      val bucket = cfgClient.getDynamicBucket(bucketId)
      bucketConfigs.put(bucketId, ConfigFactory.parseMap(bucket.getKeys))
      logger.info(s"Fetched config for bucket: $bucketId [$bucket]")

      bucket.addListener(new BucketUpdateListener() {
        override def updated(oldBucket: Bucket, newBucket: Bucket): Unit = {
          logger.info(s"dynamic bucket $bucketId updated")
          bucketConfigs.put(bucketId, ConfigFactory.parseMap(newBucket.getKeys))

          this.synchronized {
            applyConfig(overlayConfigs(bucketConfigs.values.toList: _*))
          }
        }

        override def connected(s: String): Unit = logger.debug(s"dynamic bucket $bucketId connected.")

        override def disconnected(s: String, e: Exception): Unit = logger.debug(s"dynamic bucket $bucketId dis-connected.")

        override def deleted(s: String): Unit = logger.info(s"dynamic bucket $bucketId deleted.")
      })

    })

    bucketConfigs.values.toList
  }


  def init() = {
    logger.info("Config Init")
    try {
      val configs = readConfigs()
      this.synchronized {
        applyConfig(overlayConfigs(configs: _*))
      }
    } catch {
      case uhe@(_: UnknownHostException | _: IOException | _: ConfigServiceException) =>
        if (NetworkUtils.getHostname.contains("local"))
          logger.warn(s"Offline Mode, Unable to reach $configHost")
        else
          throw uhe;
      case e: Throwable =>
        throw e;

    }
  }

  def terminate() = {
    logger.info("Connekt config client terminating")
    Option(cfgClient).foreach(_.shutdown())
  }

}

object KloudConfig {

  type BucketGroupName = String
  type BucketIds = Seq[String]

  private var instances: Map[BucketGroupName, IKloudConfig] = Map()

  def apply(configHost: String = "10.47.0.101", configPort: Int = 80)
           (bucketIdMap: Map[BucketGroupName, BucketIds]) = {

    this.synchronized {
      bucketIdMap.foreach {
        case (grpName, grpIds) =>
          instances.get(grpName) match {
            case Some(x) => //do nothing
            case None =>
              val instance = new KloudConfig(configHost, configPort)(grpIds)
              instance.init()
              instances += grpName -> instance
          }
      }
    }
  }

  def getBucket(name: BucketGroupName): Option[IKloudConfig] = {
    instances.get(name)
  }

}