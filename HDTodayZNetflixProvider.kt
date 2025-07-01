package com.cloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class HDTodayZNetflixProvider : MainAPI() {
    override var mainUrl = "https://hdtodayz.to"
    override var name = "HDTodayZ (Netflix Style)"
    override val hasMainPage = true
    override var lang = "ar"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override val useAutoLoadMore = true

    override val mainPage = mainPageOf(
        "$mainUrl/movies" to "أفلام شائعة",
        "$mainUrl/tv-series" to "مسلسلات رائجة",
        "$mainUrl/trending" to "الأكثر مشاهدة",
        "$mainUrl/genre/action" to "أفلام أكشن",
        "$mainUrl/genre/comedy" to "كوميديا",
        "$mainUrl/genre/history" to "أفلام تاريخية",
        "$mainUrl/genre/drama" to "دراما"
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.film-name a")?.text()?.trim() ?: return null
        val href = fixUrl(this.selectFirst("a")?.attr("href") ?: return null)
        val posterUrl = this.selectFirst("img.film-poster-img")?.attr("data-src")
        val quality = this.selectFirst("div.pick.film-poster-quality")?.text()?.trim()
        val tvType = if (href.contains("/tv-series/")) TvType.TvSeries else TvType.Movie
        val episode = this.selectFirst("div.tick-eps")?.text()?.trim()

        return newAnimeSearchResponse(title, href, tvType) {
            this.posterUrl = posterUrl
            this.quality = quality
            addDubStatus(dubStatus = false, subStatus = true)
            if (tvType == TvType.TvSeries) {
                addSub(episode)
            }
        }
    }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get("${request.data}?page=$page").document
        val items = document.select("div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    items,
                    isHorizontalImages = true,
                    posterWidth = 300,
                    posterHeight = 450
                )
            )
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h2.heading-name")?.text()?.trim() ?: return null
        val poster = document.selectFirst("img.film-poster-img")?.attr("src")
        val background = poster
        val tags = document.select("div.row-line:contains(Genre) a").map { it.text() }
        val year = document.selectFirst("div.row-line:contains(Release)")?.text()
            ?.substringAfter(":")?.trim()?.toIntOrNull()
        val description = document.selectFirst("div.description")?.text()?.trim()
        val rating = document.selectFirst("span.item.rating")?.text()?.trim()?.toRatingInt()
        val type = if (url.contains("/tv-series/")) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            val episodes = document.select("ul.listing.items li").mapNotNull {
                val episode = it.selectFirst("a")?.attr("title")?.trim() ?: return@mapNotNull null
                val episodeUrl = fixUrl(it.selectFirst("a")?.attr("href") ?: return@mapNotNull null)
                Episode(episodeUrl, episode)
            }.reversed()

            newTvSeriesLoadResponse(title, url, type, episodes) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = true
            }
        } else {
            newMovieLoadResponse(title, url, type, url) {
                this.posterUrl = poster
                this.backgroundPosterUrl = background
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = true
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.play-video iframe").forEach { iframe ->
            val url = iframe.attr("src")
            if (url.isNotEmpty()) {
                // 🔄 استخدم loadExtractor لتحميل الرابط الداخلي مباشرةً
                loadExtractor(url, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
