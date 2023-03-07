package cc.drny.lanzou.data.update

data class UpdateResponse(
    val status: String,
    val msg: String,
    val update: Update? = null
) {
    data class Update(
        val name: String,
        val code: Int,
        val url: String,
        val content: String,
        val date: String,
        val size: String
    )
}

