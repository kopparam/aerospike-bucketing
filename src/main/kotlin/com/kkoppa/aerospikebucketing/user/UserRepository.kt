package com.kkoppa.aerospikebucketing.user

import com.aerospike.client.AerospikeClient
import com.aerospike.client.async.NioEventLoops
import com.aerospike.client.policy.ClientPolicy
import com.aerospike.client.reactor.AerospikeReactorClient
import com.aerospike.mapper.annotations.*
import com.aerospike.mapper.tools.AeroMapper
import org.springframework.stereotype.Repository
import java.util.*


@Repository
class UserRepository(val aerospikeClient: AerospikeClient) {
    fun save(user: User): UserDAO? {

        // Create a ClientPolicy.
        // Create a ClientPolicy.
        val policy = ClientPolicy()
// Set event loops to use in asynchronous commands.
// Set event loops to use in asynchronous commands.
        policy.eventLoops = NioEventLoops(1)

// Instantiate an AerospikeReactorClient which embeds an AerospikeClient.

// Instantiate an AerospikeReactorClient which embeds an AerospikeClient.
        val client = AerospikeClient(policy, "localhost", 3000)
        val reactorClient = AerospikeReactorClient(client)


        val id = UUID.randomUUID().toString()

        val aeroMapper = AeroMapper.Builder(aerospikeClient)
            .build()

        val userDAO = UserDAO(
            id,
            user.externalIds?.map { ExternalIdDAO("${it.id}:${it.type}", it.id, it.type.name, id) }?.toList(),
            user.data
        )

        aeroMapper.save(aerospikeClient.writePolicyDefault, userDAO)
        userDAO.externalIds?.forEach { aeroMapper.save(aerospikeClient.writePolicyDefault, it) }

        val read = aeroMapper.read(UserDAO::class.java, id)
        return read
    }
}

@AerospikeRecord(namespace = "test", set = "user", sendKey = true)
data class UserDAO(
    @AerospikeKey val id: String = "",
    @ParamFrom("externalIds") @AerospikeEmbed(type = AerospikeEmbed.EmbedType.LIST) val externalIds: List<ExternalIdDAO>? = null,
    @ParamFrom("data") val data: String? = "",
)

@AerospikeRecord(namespace = "test", set = "externalIds", sendKey = true)
data class ExternalIdDAO(
    @AerospikeKey val pk: String = "",
    @ParamFrom("externalId") val externalId: String = "",
    @ParamFrom("type") val type: String = "",
    @ParamFrom("id") val id: String = "",
)
