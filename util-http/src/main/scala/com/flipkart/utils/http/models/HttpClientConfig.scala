package com.flipkart.utils.http.models

import com.sun.istack.internal.NotNull
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.conn.{DnsResolver, HttpConnectionFactory, ManagedHttpClientConnection, SchemePortResolver}

case class HttpClientConfig (name: String, @NotNull ttlInMillis: Long, @NotNull maxConnections: Int,
                             @NotNull processQueueSize: Int, @NotNull connectionTimeoutInMillis: Integer,
                             @NotNull socketTimeoutInMillis: Integer, sslConfig: Option[SSLConfig] = None,
                             connectionFactory: HttpConnectionFactory[HttpRoute, ManagedHttpClientConnection] = null,
                             schemePortResolver: SchemePortResolver = null, dnsResolver: DnsResolver = null) {
}
