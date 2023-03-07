package cc.drny.lanzou.data.upload

sealed class UploadState(val upload: Upload)

class Insert(upload: Upload): UploadState(upload)

class Progress(upload: Upload) : UploadState(upload)

class Stop(upload: Upload): UploadState(upload)

class Error(upload: Upload, val msg: String?): UploadState(upload)

class Completed(upload: Upload): UploadState(upload)
