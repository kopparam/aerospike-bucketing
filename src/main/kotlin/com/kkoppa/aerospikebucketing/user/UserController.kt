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

@Controller
@RequestMapping("/user")
class UserController(val userRepository: UserRepository) {
    @PostMapping
    @ResponseBody
    fun createUser(@RequestBody user: User): User {
        val userDAO = userRepository.save(user)

        return User(
            data = userDAO?.data,
            id = userDAO?.id,
            externalIds = userDAO?.externalIds?.map {
                ExternalId(id = it.externalId!!, type = ExternalIdType.valueOf(it.type!!))
            },
        )
    }

    @GetMapping
    @ResponseBody
    fun getUser(
        @RequestParam("goCustomerId") goCustomerId: String?,
        @RequestParam("payAccountId") payAccountId: String?,
    ): User {
        if (!goCustomerId.isNullOrBlank()) {
            return userRepository.getByGoCustomerId(goCustomerId).toUser()
        }

        if (!payAccountId.isNullOrBlank()) {
            return userRepository.getByPayAccountId(payAccountId).toUser()
        }

        throw BadRequestException("goCustomerId or payAccountId is required")
    }

    @GetMapping("/{id}")
    @ResponseBody
    fun getUserById(@PathVariable("id") id: String): User {
        return userRepository.getById(id).toUser()
    }
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
}

class BadRequestException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class NotFoundException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
