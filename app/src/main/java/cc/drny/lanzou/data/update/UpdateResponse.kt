package cc.drny.lanzou.data.update

data class UpdateResponse(
    val status: String,
    val msg: String,
    val update: Update? = null
) {
    data class Update(
        val versionName: String,
        val versionCode: Int,
        val url: String,
        val content: String,
        val date: String,
        val size: String
    )
}

