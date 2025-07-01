package com.hicham8367.hdtodayz

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.*

class HDTodayZ : MainAPI() {
    override var mainUrl = "https://hdtodayz.com"
    override var name = "HDTodayZ"
    override var lang = "en"

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search?q=$query").document
        return doc.select(".movie-card").map {
            val title = it.selectFirst(".title")!!.text()
            val link = it.selectFirst("a")!!.absUrl("href")
            val poster = it.selectFirst("img")!!.absUrl("src")
            newMovieSearchResponse(title, link) { this.posterUrl = poster }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")!!.text()
        val poster = doc.selectFirst(".poster img")!!.absUrl("src")
        val episodes = listOf(Episode("Full Movie", url))
        return newMovieLoadResponse(title, url, episodes) {
            this.posterUrl = poster
        }
    }

    override suspend fun loadLinks(
        url: String, referer: String?, isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val doc = app.get(url).document
        doc.select(".server-list a").forEach {
            val serverName = it.text()
            val videoUrl = it.absUrl("href")
            callback(ExtractorLink(serverName, videoUrl, serverName, videoUrl, false))
        }
    }
}
