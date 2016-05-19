package com.flipkart.utils.http.models

import com.sun.istack.internal.NotNull

case class HttpClientConfig (name: String, @NotNull ttlInMillis: Long, @NotNull maxConnections: Int,
                             @NotNull processQueueSize: Int, @NotNull connectionTimeoutInMillis: Integer,
                             @NotNull socketTimeoutInMillis: Integer, sslConfig: Option[SSLConfig] = None) {
}
