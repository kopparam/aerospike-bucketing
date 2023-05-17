package com.kkoppa.aerospikebucketing.user

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.core.publisher.whenComplete
import java.util.*

@Controller
@RequestMapping("/user")
class UserController(
    val userRepository: UserRepository,
    val externalIdRepository: ExternalIdRepository,
) {
    val log = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    @ResponseBody
    fun createUser(@RequestBody user: User): Mono<User> {
        if (user.externalIds == null) {
            throw BadRequestException("At least one External id is required")
        } else {
            return Flux.concat(
                user.externalIds.map { externalId ->
                    externalIdRepository.existsById(
                        ExternalIdKey(
                            externalId = externalId.id,
                            type = externalId.type,
                        ),
                    ).mapNotNull {
                        if (it) {
                            externalId
                        } else {
                            null
                        }
                    }
                },
            ).collectList()
                .flatMap { existingExternalIds ->
                    if (existingExternalIds.isNotEmpty()) {
                        Mono.error(
                            BadRequestException(
                                "External id already exists ${
                                    existingExternalIds.map { "${it?.type}:${it?.id}" }.joinToString(",")
                                }",
                            ),
                        )
                    } else {
                        val userDao = mapUserToUserDAO(user)

                        user.externalIds.map {
                            ExternalIdDataDao(
                                ExternalIdKey(it.id, it.type),
                                userDao.id,
                            )
                        }.map { externalIdRepository.save(it) }.whenComplete()
                            .then(userRepository.save(userDao).map { mapUserDaoToUser(it) })

                    }
                }
        }


    }

    private fun mapUserDaoToUser(userDataDao: UserDataDao) = User(
        data = userDataDao.data,
        id = userDataDao.id,
        externalIds = userDataDao.externalIds.map {
            ExternalId(id = it.externalId, type = it.type)
        },
    )

    private fun mapUserToUserDAO(user: User): UserDataDao {
        return UserDataDao(
            id = UUID.randomUUID().toString(),
            data = user.data,
            externalIds = user.externalIds?.map {
                ExternalIdKey(externalId = it.id, type = it.type)
            } ?: emptyList(),
        )
    }

    @GetMapping
    @ResponseBody
    fun getUser(
        @RequestParam("goCustomerId") goCustomerId: String?,
        @RequestParam("payAccountId") payAccountId: String?,
    ): Mono<User> {
        if (!goCustomerId.isNullOrBlank()) {
            return externalIdRepository.findById(
                ExternalIdKey(
                    externalId = goCustomerId,
                    type = ExternalIdType.GO_CUSTOMER_ID,
                ),
            )
                .onErrorMap { ServerErrorException("Server Error", it) }
                .flatMap { userRepository.findById(it.id) }
                .map { mapUserDaoToUser(it) }
        }

        if (!payAccountId.isNullOrBlank()) {
            return externalIdRepository.findById(
                ExternalIdKey(
                    externalId = payAccountId,
                    type = ExternalIdType.PAY_ACCOUNT_ID,
                ),
            )
                .onErrorMap { ServerErrorException("Server Error", it) }
                .flatMap { userRepository.findById(it.id) }
                .map { mapUserDaoToUser(it) }
        }

        throw BadRequestException("goCustomerId or payAccountId is required")
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getUserById(@PathVariable("id") id: String): Mono<User> {
        return userRepository.findById(id).map { mapUserDaoToUser(it) }
    }

    @PostMapping("/externalId")
    @ResponseBody
    fun saveExternalId(
        @RequestBody externalId: ExternalIdDao,
    ): Mono<ExternalIdDao> {
        return externalIdRepository.save(
            ExternalIdDataDao(
                ExternalIdKey(
                    externalId.externalId.id,
                    externalId.externalId.type
                ), externalId.id
            )
        ).map {
            ExternalIdDao(
                it.id,
                ExternalId(it.externalIdKey.externalId, it.externalIdKey.type)
            )
        }
    }

    @GetMapping("/externalId/{externalIdType}/{externalId}")
    @ResponseBody
    fun getUserByExternalId(
        @PathVariable("externalId") externalId: String,
        @PathVariable("externalIdType") externalIdType: ExternalIdType,
    ): Mono<User> {
        return externalIdRepository.findById(ExternalIdKey(externalId = externalId, type = externalIdType))
            .onErrorMap { ServerErrorException("Server Error", it) }
            .flatMap { userRepository.findById(it.id) }
            .map { mapUserDaoToUser(it) }
    }

    data class User(
        val id: String? = null,
        val externalIds: List<ExternalId>? = null,
        val data: String?,
    )

    data class ExternalIdDao(
        val id: String,
        val externalId: ExternalId
    )

    data class ExternalId(
        val id: String,
        val type: ExternalIdType,
    )

    enum class ExternalIdType {
        UNKNOWN,
        GO_CUSTOMER_ID,
        PAY_ACCOUNT_ID,
        LENDING_PLATFORM_ID,
    }

    @RestControllerAdvice
    class CustomExceptionHandler {
        @ExceptionHandler(BadRequestException::class)
        fun handleBadRequestException(ex: BadRequestException): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, ex.message ?: "").build()
            return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
        }

        @ExceptionHandler(NotFoundException::class)
        fun handleNotFoundException(ex: NotFoundException): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse.builder(ex, HttpStatus.NOT_FOUND, ex.message ?: "").build()
            return ResponseEntity(errorResponse, HttpStatus.NOT_FOUND)
        }

        @ExceptionHandler(ServerErrorException::class)
        fun handleServerErrorException(ex: ServerErrorException): ResponseEntity<ErrorResponse> {
            val errorResponse =
                ErrorResponse.builder(ex, HttpStatus.INTERNAL_SERVER_ERROR, ex.message ?: "").build()
            return ResponseEntity(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    class BadRequestException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }

    class NotFoundException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }

    class ServerErrorException : RuntimeException {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
    }
}
