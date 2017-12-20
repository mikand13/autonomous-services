package org.mikand.autonomous.services.gateway.utils

import io.restassured.specification.RequestSpecification

/**
 * @author Anders Mikkelsen
 * @version 20.12.17 11:41
 */
interface WhenSupport {
    fun RequestSpecification.When(): RequestSpecification {
        return this.`when`()
    }
}