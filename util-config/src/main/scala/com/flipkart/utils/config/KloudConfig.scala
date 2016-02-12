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
import collection.JavaConversions._

class KloudConfig(configHost: String, configPort: Int)
                 (bucketIds: BucketIds) extends ConfigAccumulator with IKloudConfig {

  val logger = LoggerFactory.getLogger(getClass)

  var cfgClient: ConfigClient = null

  /** BucketId -> Config */
  val bucketConfigs = new java.util.TreeMap[Int, Config]()

  private def readConfigs(): List[Config] = {
    cfgClient = new ConfigClient(configHost, configPort, 1, 30000)
    logger.info(s"Buckets to fetch config: [${bucketIds.toList}]")

    bucketIds.zipWithIndex.foreach {
      case (bucketId, bucketPosition) =>

        val bucket = cfgClient.getDynamicBucket(bucketId)
        bucketConfigs.put(bucketPosition, ConfigFactory.parseMap(bucket.getKeys))
        logger.debug(s"Fetched config for bucket: [$bucketId], POS[$bucketPosition], Values : [$bucket]")

        bucket.addListener(new BucketUpdateListener() {
          override def updated(oldBucket: Bucket, newBucket: Bucket): Unit = {
            logger.info(s"Dynamic Bucket $bucketId Updated at POS[$bucketPosition]")
            bucketConfigs.put(bucketPosition, ConfigFactory.parseMap(newBucket.getKeys))

            this.synchronized {
              applyConfig(overlayConfigs(bucketConfigs.values.toSeq: _*))
            }
          }

          override def connected(s: String): Unit = logger.info(s"Dynamic Bucket $bucketId connected.")

          override def disconnected(s: String, e: Exception): Unit = logger.error(s"Dynamic Bucket $bucketId dis-connected.")

          override def deleted(s: String): Unit = logger.info(s"Dynamic Bucket $bucketId deleted.")
        })

    }

    bucketConfigs.values.toList
  }


  def init() = {
    logger.info("KloudConfig Init")
    try {
      val configs = readConfigs()
      this.synchronized {
        applyConfig(overlayConfigs(configs: _*))
      }
    } catch {
      case uhe: ConfigServiceException if uhe.getCause.isInstanceOf[UnknownHostException] =>
        if (NetworkUtils.getHostname.contains("local"))
          logger.warn(s"Offline Mode, Unable to reach $configHost")
        else
          throw uhe
      case e: Throwable =>
        throw e;

    }
  }

  def terminate() = {
    logger.info("ConfigService Client terminating")
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