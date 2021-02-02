package org.muellners.finscale.accounting.security.oauth2

import org.muellners.finscale.accounting.config.oauth2.OAuth2Properties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.security.jwt.crypto.sign.RsaVerifier
import org.springframework.security.jwt.crypto.sign.SignatureVerifier
import org.springframework.security.oauth2.common.exceptions.InvalidClientException
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

/**
 * Client fetching the public key from UAA to create a [SignatureVerifier].
 */
@Component
class UaaSignatureVerifierClient(
    discoveryClient: DiscoveryClient,
    @param:Qualifier("loadBalancedRestTemplate") private val restTemplate: RestTemplate,
    protected val oAuth2Properties: OAuth2Properties
) : OAuth2SignatureVerifierClient {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        // Load available UAA servers
        discoveryClient.services
    }

    /**
     * Fetches the public key from the UAA.
     *
     * @return the public key used to verify JWT tokens; or `null`.
     */
    override fun getSignatureVerifier(): SignatureVerifier? =
        try {
            val request = HttpEntity<Void>(HttpHeaders())
            val key = restTemplate
                .exchange(getPublicKeyEndpoint(), HttpMethod.GET, request, Map::class.java).body!!["value"] as String
            RsaVerifier(key)
        } catch (ex: IllegalStateException) {
            log.warn("could not contact UAA to get public key")
            null
        }

    /**
     * Returns the configured endpoint URI to retrieve the public key.
     *
     * @return the configured endpoint URI to retrieve the public key.
     */
    private fun getPublicKeyEndpoint() =
        oAuth2Properties.signatureVerification.publicKeyEndpointUri
            ?: throw InvalidClientException("no token endpoint configured in application properties")
}
