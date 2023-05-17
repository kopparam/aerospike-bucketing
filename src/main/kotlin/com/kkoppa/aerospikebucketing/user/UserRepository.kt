package com.kkoppa.aerospikebucketing.user

import org.springframework.core.convert.converter.Converter
import org.springframework.data.aerospike.mapping.Document
import org.springframework.data.aerospike.mapping.Field
import org.springframework.data.aerospike.repository.ReactiveAerospikeRepository
import org.springframework.data.annotation.Id
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.stereotype.Repository

// Use this for composite key
// https://github.com/aerospike-community/spring-data-aerospike-demo/blob/main/docs_processed/composite-primary-key.adoc

@Repository
interface UserRepository : ReactiveAerospikeRepository<UserDataDao, String>

@Repository
interface ExternalIdRepository : ReactiveAerospikeRepository<ExternalIdDataDao, ExternalIdKey>

@Document(collection = "user")
data class UserDataDao(
    @Id
    val id: String,

    @Field
    val data: String?,

    @Field("externalIds")
    val externalIds: List<ExternalIdKey>,
)

@Document(collection = "externalIds")
data class ExternalIdDataDao(
    @Id
    val externalIdKey: ExternalIdKey,

    @Field
    val id: String,
)

data class ExternalIdKey(
    val externalId: String,
    val type: UserController.ExternalIdType,
) {
    @WritingConverter
    enum class ExternalIdKeyToStringConverter : Converter<ExternalIdKey, String> {
        INSTANCE, ;

        override fun convert(source: ExternalIdKey): String {
            return "${source.type}:${source.externalId}"
        }
    }

    @ReadingConverter
    enum class StringToExternalIdKeyConverter : Converter<String, ExternalIdKey> {
        INSTANCE, ;

        override fun convert(source: String): ExternalIdKey {
            val parts = source.split(":")
            return ExternalIdKey(externalId = parts[1], type = UserController.ExternalIdType.valueOf(parts[0]))
        }
    }
}
