package com.kkoppa.aerospikebucketing.user

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
import reactor.core.publisher.Mono
import java.util.*

@Controller
@RequestMapping("/user")
class UserController(
    val userRepository: UserRepository,
    val externalIdRepository: ExternalIdRepository,
) {
    @PostMapping
    @ResponseBody
    fun createUser(@RequestBody user: User): Mono<User> {
        val userDao = mapUserToUserDAO(user)
        val savedUser = userRepository.save(userDao)
        externalIdRepository.saveAll<ExternalIdDataDao> {
            user.externalIds?.map { ExternalIdDataDao(ExternalIdKey(it.id, it.type), userDao.id) }
        }

        return savedUser.map { userDataDao ->
            mapUserDaoToUser(userDataDao)
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
