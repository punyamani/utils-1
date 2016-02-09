package com.flipkart.utils.config.test

import com.flipkart.utils.config.KloudConfig
import org.scalatest._

/**
 *
 *
 * @author kinshuk.bairagi
 */
class KloudConfigTest   extends FlatSpec  with Matchers with OptionValues with Inside with Inspectors with BeforeAndAfterAll {

  "KloudConfig" should "connect succesfully" in {
    val intance = new KloudConfig( "config-service.nm.flipkart.com", 80)(Seq("fk-connekt-credentials1", "fk-connekt-root"))
    intance.init()
  }
}
