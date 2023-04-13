package com.kkoppa.aerospikebucketing.user

import com.aerospike.client.AerospikeClient
import com.aerospike.client.AerospikeException
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.policy.Policy
import com.aerospike.client.policy.RecordExistsAction
import com.aerospike.client.policy.WritePolicy
import com.aerospike.mapper.annotations.AerospikeBin
import com.aerospike.mapper.annotations.AerospikeEmbed
import com.aerospike.mapper.annotations.AerospikeKey
import com.aerospike.mapper.annotations.AerospikeRecord
import com.aerospike.mapper.annotations.ParamFrom
import com.aerospike.mapper.tools.AeroMapper
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class UserRepository(val applicationContext: ApplicationContext, val aeroMapper: AeroMapper) {
    private val log = LoggerFactory.getLogger(UserRepository::class.java.name)

    fun save(user: User): UserDAO? {
        val id = UUID.randomUUID().toString()
        val userDAO = UserDAO(
            id = id,
            externalIds = user.externalIds?.map { externalId ->
                ExternalIdDao(
                    externalId = externalId.id,
                    type = externalId.type.name,
                )
            }
                ?.toList(),
            data = user.data,
        )

        val writePolicy =
            applicationContext.getBean(AerospikeConfig.AEROSPIKE_DEFAULT_WRITE_POLICY_BEAN, WritePolicy::class.java)
        writePolicy.recordExistsAction = RecordExistsAction.CREATE_ONLY

        val saved = mutableListOf<ExternalIdMapDao>()
        try {
            user.externalIds?.forEach {
                val externalIdMapDao = ExternalIdMapDao(
                    id = id,
                    typeParam = it.type.name,
                    externalIdParam = it.id,
                    pk = "${it.type}:${it.id}",
                )
                try {
                    aeroMapper.save(writePolicy, externalIdMapDao)
                } catch (e: AerospikeException) {
                    log.error("ExternalId: ${it.id} already exists", e)
                    throw e
                }
                saved.add(externalIdMapDao)
            }
        } catch (e: AerospikeException) {
            val deletePolicy =
                applicationContext.getBean(AerospikeConfig.AEROSPIKE_DEFAULT_WRITE_POLICY_BEAN, WritePolicy::class.java)
            deletePolicy.durableDelete = true
            saved.forEach { aeroMapper.delete(it) }
            throw BadRequestException(
                "One of the externalIds already exists",
                e,
            )
        }

        aeroMapper.save(
            writePolicy,
            userDAO,
        )

        val readPolicy =
            applicationContext.getBean(AerospikeConfig.AEROSPIKE_DEFAULT_READ_POLICY_BEAN, Policy::class.java)
        return aeroMapper.read(readPolicy, UserDAO::class.java, id)
    }
}

@AerospikeRecord(namespace = "test", set = "user", sendKey = true)
data class UserDAO(
    @AerospikeKey val id: String = "",
    @ParamFrom("externalIds")
    @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST)
    val externalIds: List<ExternalIdDao>? = null,
    @ParamFrom("data") val data: String? = "",
)

open class ExternalIdDao(
    @ParamFrom("externalId") val externalId: String? = "",
    @ParamFrom("type") val type: String? = "",
)

@AerospikeRecord(namespace = "test", set = "externalIds", sendKey = true)
open class ExternalIdMapDao(
    @AerospikeKey val pk: String = "",
    @ParamFrom("id") val id: String = "",
    @ParamFrom("externalId")
    @AerospikeBin(name = "externalId")
    val externalIdParam: String = "",
    @ParamFrom("type")
    @AerospikeBin(name = "type")
    val typeParam: String = "",
) : ExternalIdDao(externalIdParam, typeParam)

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
