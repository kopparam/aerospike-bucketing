package com.kkoppa.aerospikebucketing

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AerospikeBucketingApplication

fun main(args: Array<String>) {
    runApplication<AerospikeBucketingApplication>(*args)
}
