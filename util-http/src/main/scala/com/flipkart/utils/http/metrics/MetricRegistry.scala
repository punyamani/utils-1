package com.flipkart.utils.http.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics._

/**
 * Created by kinshuk.bairagi on 11/02/16.
 */
object MetricRegistry {

  val registry: MetricRegistry = new com.codahale.metrics.MetricRegistry()

  val jmxReporter: JmxReporter = JmxReporter
    .forRegistry(registry)
    .inDomain("fk.metrics")
    .convertDurationsTo(TimeUnit.MILLISECONDS)
    .convertRatesTo(TimeUnit.SECONDS)
    .build()

  jmxReporter.start()

}


