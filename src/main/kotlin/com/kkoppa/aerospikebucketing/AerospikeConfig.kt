package com.kkoppa.aerospikebucketing

import com.aerospike.client.AerospikeClient
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.policy.Policy
import com.aerospike.client.policy.WritePolicy
import com.aerospike.mapper.tools.AeroMapper
import jakarta.annotation.PreDestroy
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class AerospikeConfig {

    companion object {
        const val AEROSPIKE_DEFAULT_WRITE_POLICY_BEAN = "aerospikeDefaultWritePolicy"
        const val AEROSPIKE_DEFAULT_READ_POLICY_BEAN = "aerospikeDefaultReadPolicy"
    }

    @Bean
    @Scope("prototype")
    fun aerospikeDefaultClientPolicy(): ClientPolicy {
        return ClientPolicy()
    }

    @Bean
    fun aerospikeClient(clientPolicy: ClientPolicy): AerospikeClient {
//      No Need for this as JavaObjectMapper for Aerospike will send the key as a bin by default
//        clientPolicy.writePolicyDefault.sendKey = true
        return AerospikeClient(clientPolicy, "127.0.0.1", 3000)
    }

    @Bean
    fun aerospikeObjectMapper(aerospikeClient: AerospikeClient): AeroMapper {
        return AeroMapper.Builder(aerospikeClient).build()
    }

    @Bean
    @Scope("prototype")
    @Qualifier(AEROSPIKE_DEFAULT_WRITE_POLICY_BEAN)
    fun aerospikeDefaultWritePolicy(aerospikeClient: AerospikeClient): WritePolicy {
        return aerospikeClient.writePolicyDefault
    }

    @Bean
    @Scope("prototype")
    @Qualifier(AEROSPIKE_DEFAULT_READ_POLICY_BEAN)
    fun aerospikeDefaultReadPolicy(aerospikeClient: AerospikeClient): Policy {
        return aerospikeClient.readPolicyDefault
    }

//    @Bean
//    fun aerospikeReactorClient(): AerospikeReactorClient {
//
//        val policy = aerospikeDefaultClientPolicy()
//        policy.eventLoops = NioEventLoops(1)
//
//        val client = AerospikeClient(policy, "localhost", 3000)
//
//        // Create an Aerospike reactor client.
//        val reactorClient = AerospikeReactorClient(client)
//
//        return AerospikeReactorClient(aerospikeClient())
//    }
}

@Configuration
class AerospikeCleanup(val aerospikeClient: AerospikeClient) {
    @PreDestroy
    fun cleanUp() {
        aerospikeClient.close()
    }
}
