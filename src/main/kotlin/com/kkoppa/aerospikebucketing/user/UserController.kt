package com.kkoppa.aerospikebucketing.user

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody

@Controller
@RequestMapping("/user")
class UserController(val userRepository: UserRepository) {
    @PostMapping
    @ResponseBody
    fun createUser(@RequestBody user: User): User {
        val userDAO = userRepository.save(user)


        return User(data = userDAO?.data, id = userDAO?.id,
            externalIds = userDAO?.externalIds?.map {
                ExternalId(id = it.externalId, type = ExternalIdType.valueOf(it.type))
            })
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
