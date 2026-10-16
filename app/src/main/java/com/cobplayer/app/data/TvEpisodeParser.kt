package com.cobplayer.app.data

data class ParsedEpisode(
    val show: String,
    val season: Int,
    val episode: Int,
    val title: String
)

object TvEpisodeParser {

    // Matches: "Show Name S02E07", "Show.Name.S02E07", "Show - 2x07 - Title",
    // "Show Name - S02E07", "Show 2x07", "Show.S02.E07"
    private val patterns = listOf(
        Regex("""^(.+?)[\s._-]+[Ss](\d{1,2})[Ee](\d{1,2})(?:[\s._-]+(.+))?$"""),
        Regex("""^(.+?)[\s._-]+(\d{1,2})x(\d{1,2})(?:[\s._-]+(.+))?$"""),
        Regex("""^(.+?)[\s._-]+[Ss]eason[\s._-]*(\d{1,2})[\s._-]+[Ee]pisode[\s._-]*(\d{1,2})(?:[\s._-]+(.+))?$""")
    )

    fun parse(filename: String): ParsedEpisode? {
        val base = filename.substringBeforeLast('.')
        for (rx in patterns) {
            val m = rx.matchEntire(base) ?: continue
            val show = m.groupValues[1].trim().replace('.', ' ').replace('_', ' ')
            val season = m.groupValues[2].toIntOrNull() ?: continue
            val episode = m.groupValues[3].toIntOrNull() ?: continue
            val title = m.groupValues.getOrNull(4).orEmpty()
                .trim().replace('.', ' ').replace('_', ' ')
            return ParsedEpisode(show, season, episode, title)
        }
        return null
    }
}
