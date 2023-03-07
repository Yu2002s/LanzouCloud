package cc.drny.lanzou.data.user

data class User(
    val username: String,
    val password: String = ""
) {
    var uid: Int = 0
}
