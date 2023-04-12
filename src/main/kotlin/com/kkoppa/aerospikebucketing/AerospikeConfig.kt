package com.kkoppa.aerospikebucketing

import com.aerospike.client.AerospikeClient
import com.aerospike.client.policy.ClientPolicy
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class AerospikeConfig {
    @Bean
    fun aerospikeClient(): AerospikeClient {

        val policy = ClientPolicy()
        policy.writePolicyDefault.sendKey = true
        policy.readPolicyDefault.sendKey = true
        return AerospikeClient(policy, "127.0.0.1", 3000)
    }

    @PreDestroy
    fun cleanUp() {
        aerospikeClient().close()
    }
}