package com.flipkart.utils.config

import com.typesafe.config.{ConfigFactory, Config, ConfigException}
import org.slf4j.LoggerFactory

/**
 * Created by kinshuk.bairagi on 09/02/16.
 */
trait IKloudConfig {

  private val _logger = LoggerFactory.getLogger(getClass)

  private var appConfig: Config = ConfigFactory.empty()

  def applyConfig(cfg:Config): Unit = {
    _logger.info(s"Config Applied: $cfg")
    appConfig = cfg
  }

  def get[V](k: String): Option[V] = try {
    Some(appConfig.getAnyRef(k).asInstanceOf[V])
  } catch {
    case _: ConfigException.Missing => None
  }

  def getOrElse[T](key: String, default: T) = try {
    appConfig.getAnyRef(key).asInstanceOf[T]
  } catch {
    case e: ConfigException.Missing => default
  }

  def getString(k: String): Option[String] = try {
    Some(appConfig.getString(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getInt(k: String): Option[Int] = try {
    Some(appConfig.getInt(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getDouble(k: String): Option[Double] = try {
    Some(appConfig.getDouble(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getBoolean(k: String): Option[Boolean] = try {
    Some(appConfig.getBoolean(k))
  } catch {
    case _: ConfigException.Missing => None
  }

  def getConfig(k: String): Option[Config] = try {
    Some(appConfig.getConfig(k))
  } catch {
    case _: ConfigException.Missing => None
  }

}
