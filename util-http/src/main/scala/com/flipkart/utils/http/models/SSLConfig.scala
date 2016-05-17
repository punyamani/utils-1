package com.flipkart.utils.http.models

import javax.net.ssl.{SSLContext, HostnameVerifier}

import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, TrustStrategy}

case class SSLConfig(sslContext:SSLContext, supportedProtocols: Array[String] = Array.empty,
                     supportedCipherSuites: Array[String] = Array.empty,
                     trustStrategy: Option[TrustStrategy] = None,
                     hostnameVerifier: HostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier) {
}
