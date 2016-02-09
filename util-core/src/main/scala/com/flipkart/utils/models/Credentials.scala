package com.flipkart.utils.models

/**
 * Created by kinshuk.bairagi on 23/09/14.
 */
case class Credentials(username: String, password: String) {

  def isEmpty: Boolean = {
    username != null && username.nonEmpty && password != null
  }

}

object Credentials {
  val EMPTY = Credentials(null, null)
}
