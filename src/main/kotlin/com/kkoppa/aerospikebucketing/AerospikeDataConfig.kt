package com.kkoppa.aerospikebucketing

import com.aerospike.client.Host
import com.aerospike.client.async.EventLoops
import com.aerospike.client.async.NioEventLoops
import com.kkoppa.aerospikebucketing.user.ExternalIdKey
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.data.aerospike.config.AbstractReactiveAerospikeDataConfiguration

@Configuration
@EnableConfigurationProperties(value = [AerospikeDataConfig.AerospikeConfigurationProperties::class])
class AerospikeDataConfig : AbstractReactiveAerospikeDataConfiguration() {
    override fun getHosts(): MutableCollection<Host> {
        return Host.parseServiceHosts("localhost:3000")
    }

    override fun nameSpace(): String {
        return "test"
    }

    override fun eventLoops(): EventLoops {
        return NioEventLoops()
    }

    override fun customConverters(): MutableList<*> {
        return mutableListOf(
            ExternalIdKey.ExternalIdKeyToStringConverter.INSTANCE,
            ExternalIdKey.StringToExternalIdKeyConverter.INSTANCE,
        )
    }

    @ConfigurationProperties("aerospike")
    class AerospikeConfigurationProperties {
        var hosts: String? = null
        var namespace: String? = null
    }
}
