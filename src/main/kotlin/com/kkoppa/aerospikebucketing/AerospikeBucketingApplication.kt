package com.kkoppa.aerospikebucketing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.aerospike.repository.config.EnableReactiveAerospikeRepositories

@SpringBootApplication
@EnableReactiveAerospikeRepositories
class AerospikeBucketingApplication

fun main(args: Array<String>) {
    runApplication<AerospikeBucketingApplication>(*args)
}
