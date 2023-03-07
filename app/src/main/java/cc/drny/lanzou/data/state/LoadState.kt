package cc.drny.lanzou.data.state

sealed class LoadState

object Loading: LoadState()

object Empty: LoadState()

class Error(val error: String): LoadState()

object Completed: LoadState()
