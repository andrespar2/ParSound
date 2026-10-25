package com.andrespar2.parsound.ui.navigation

object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val PLAYER = "player"
    const val QUEUE = "queue"
    const val EFFECTS = "effects"
    const val LICENSES = "licenses"
    const val FOLDERS = "folders"
    const val PLAYLISTS = "playlists"

    const val ALBUM = "album/{albumKey}"
    const val ARTIST = "artist/{artist}"
    const val GENRE = "genre/{genre}"
    const val FOLDER = "folder/{folderUri}/{folder}"
    const val PLAYLIST = "playlist/{playlistId}"

    fun album(albumKey: String) = "album/${android.net.Uri.encode(albumKey)}"
    fun artist(artist: String) = "artist/${android.net.Uri.encode(artist)}"
    fun genre(genre: String) = "genre/${android.net.Uri.encode(genre)}"
    fun folder(folderUri: String, folder: String) =
        "folder/${android.net.Uri.encode(folderUri)}/${android.net.Uri.encode(folder)}"
    fun playlist(id: Long) = "playlist/$id"
}
