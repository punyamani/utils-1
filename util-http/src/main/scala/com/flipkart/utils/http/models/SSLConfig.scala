package com.flipkart.utils.http.models

import javax.net.ssl.{SSLContext, HostnameVerifier}

import org.apache.http.conn.ssl.{SSLConnectionSocketFactory, TrustStrategy}

case class SSLConfig(sslContext:SSLContext, supportedProtocols: Option[Array[String]] = None,
                     supportedCipherSuites: Option[Array[String]] = None, trustStrategy: Option[TrustStrategy] = None,
                     hostnameVerifier: HostnameVerifier = SSLConnectionSocketFactory.getDefaultHostnameVerifier) {
}
