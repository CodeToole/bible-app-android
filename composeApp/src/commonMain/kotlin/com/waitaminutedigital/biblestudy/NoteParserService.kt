package com.waitaminutedigital.biblestudy

class NoteParserService(private val bibleService: IBibleService) {

    companion object {
        private val ExpandedAliases = mapOf(
            "Zach" to "Zechariah",
            "1Petr" to "1 Peter",
            "2Petr" to "2 Peter",
            "1Pt" to "1 Peter",
            "2Pt" to "2 Peter",
            "Mat" to "Matthew",
            "Mt" to "Matthew",
            "Ez" to "Ezekiel",
            "Ezek" to "Ezekiel",
            "1Thes" to "1 Thessalonians",
            "2Thes" to "2 Thessalonians",
            "1Thess" to "1 Thessalonians",
            "Eccl" to "Ecclesiastes",
            "Ex" to "Exodus",
            "Phil" to "Philippians",
            "Mal" to "Malachi",
            "Num" to "Numbers",
            "Heb" to "Hebrews"
        )

        private val ReferenceRegex = Regex(
            "^((?:[1-3]\\s*)?[A-Za-z]+(?:\\s+[A-Za-z]+)*)\\s*([0-9]+\\s*:\\s*[0-9\\s,\\-–—−;:]+)$",
            RegexOption.IGNORE_CASE
        )

        private val SingleChapterReferenceRegex = Regex(
            "^((?:[1-3]\\s*)?[A-Za-z]+(?:\\s+[A-Za-z]+)*)\\s+(\\d+[0-9\\s,\\-–—−;:]*)$",
            RegexOption.IGNORE_CASE
        )

        private val SingleChapterBooks = setOf("Obadiah", "Philemon", "2 John", "3 John", "Jude")

        private val RangeRegex = Regex("(\\d+)(?:\\s*[-–—−]\\s*(\\d+))?")

        private val ParentheticalRegex = Regex("\\([^)]*\\)")

        private val NumberedBookGluedRegex = Regex(
            "(?<=\\d)([1-3])\\s*(Petr|Peter|Pet|Pt|Thess|Thes|Sam|Samuel|Kgs|King|Kings|Chron|Chr|Chronicles|Cor|Corinthians|Tim|Timothy|Jn|John)\\b",
            RegexOption.IGNORE_CASE
        )

        private val NonNumberedBookGluedRegex = Regex(
            "(?<=\\d)(Eccl|Ecc|Rev|Revelation|Revelations|Mat|Matt|Matthew|Mt|Ez|Ezek|Ezekiel|Ex|Exod|Exo|Exodus|Phil|Philippians|Mal|Malachi|Num|Numbers|Heb|Hebrews|Zach|Zech|Zechariah|Gen|Genesis|Lev|Leviticus|Deut|Deuteronomy|Josh|Joshua|Judg|Judges|Ruth|Ezra|Neh|Nehemiah|Esth|Esther|Job|Ps|Psalm|Psalms|Prov|Proverbs|Song|Songs|Isa|Isaiah|Jer|Jeremiah|Lam|Lamentations|Dan|Daniel|Hos|Hosea|Joel|Amos|Obad|Obadiah|Jonah|Mic|Micah|Nah|Nahum|Hab|Habakkuk|Zeph|Zephaniah|Hag|Haggai|Mark|Mrk|Luke|Luk|John|Acts|Rom|Romans|Gal|Galatians|Eph|Ephesians|Col|Colossians|Tit|Titus|Philem|Philemon|Jas|James|Jude)\\b",
            RegexOption.IGNORE_CASE
        )

        private val NumberedBookSpaceFixRegex = Regex(
            "\\b([1-3])\\s*(Petr|Peter|Pet|Pt|Thess|Thes|Sam|Samuel|Kgs|King|Kings|Chron|Chr|Chronicles|Cor|Corinthians|Tim|Timothy|Jn|John)\\b",
            RegexOption.IGNORE_CASE
        )

        private val LeadingItemNumberRegex = Regex(
            "^(?:\\d+[\\.\\)]\\s*|(?!\\b[1-3]\\s?(?:Kgs|Kings|Pet|Peter|Sam|Samuel|Thes|Thess|Thessalonians|Cor|Corinthians|Chron|Chr|Chronicles|Tim|Timothy|Jn|John|Johns|Jo|Petr|Pt|Samu|Kin|Corin|Timot|Tit|Titus|Pete)\\b)\\d+\\s+)",
            RegexOption.IGNORE_CASE
        )

        private val ConcatenatedReferenceSplitRegex = Regex(
            "(?<!\\b[1-3])\\s+(?=(?:\\d+[\\.\\)]\\s+|\\d+\\s+)?(?:[1-3]\\s?)?[A-Za-z]+(?:\\s+[A-Za-z]+)*\\s+\\d+\\s*:)",
            RegexOption.IGNORE_CASE
        )

        private val ChapterMarkerRegex = Regex("(?:^|[\\s,;])(\\d+)\\s*:")

        fun resolveCanonicalBookName(rawBookName: String): String {
            if (rawBookName.isBlank()) return ""
            val trimmed = rawBookName.trim()
            ExpandedAliases[trimmed]?.let { return it }
            BibleBookAliases.getCanonicalName(trimmed)?.let { return it }
            return trimmed
        }

        fun preClean(input: String): String {
            if (input.isBlank()) return input
            // Remove parentheticals first
            var text = ParentheticalRegex.replace(input, "")
            // Standardize dashes
            text = text.replace('–', '-').replace('—', '-').replace('−', '-')
            
            // Fix glued books like "1Kings" -> "1 Kings"
            text = NumberedBookGluedRegex.replace(text, " $1 $2")
            text = NonNumberedBookGluedRegex.replace(text, " $1")
            
            // Ensure space after number: "1John" -> "1 John"
            text = NumberedBookSpaceFixRegex.replace(text, "$1 $2")
            
            return text
        }

        fun stripLeadingItemNumber(input: String): String {
            if (input.isBlank()) return input
            val trimmed = input.trim()
            val match = LeadingItemNumberRegex.find(trimmed)
            return if (match != null) {
                trimmed.substring(match.range.last + 1).trim()
            } else {
                trimmed
            }
        }

        fun parseChapterSegments(payload: String): List<ChapterSegment> {
            val results = mutableListOf<ChapterSegment>()
            if (payload.isBlank()) return results

            val matches = ChapterMarkerRegex.findAll(payload).toList()
            if (matches.isEmpty()) return results

            for (i in matches.indices) {
                val match = matches[i]
                val chapter = match.groups[1]?.value?.toIntOrNull() ?: continue

                val contentStart = payload.indexOf(':', match.range.first) + 1
                if (contentStart <= 0) continue

                val contentEnd = if (i + 1 < matches.size) matches[i + 1].range.first else payload.length
                if (contentStart > contentEnd) continue

                var rawVerseText = payload.substring(contentStart, contentEnd).trim()
                rawVerseText = rawVerseText.trimEnd(',', ';', ' ')

                val ranges = parseVerseRanges(rawVerseText)
                if (ranges.isNotEmpty()) {
                    results.add(ChapterSegment(chapter, ranges))
                }
            }
            return results
        }

        fun parseVerseRanges(verseStr: String): List<VerseRange> {
            val ranges = mutableListOf<VerseRange>()
            if (verseStr.isBlank()) return ranges

            val segments = verseStr.split(',', ';')
            for (rawSegment in segments) {
                val segment = rawSegment.trim()
                if (segment.isEmpty()) continue

                val matches = RangeRegex.findAll(segment)
                for (m in matches) {
                    val start = m.groups[1]?.value?.toIntOrNull() ?: continue
                    val end = m.groups[2]?.value?.toIntOrNull() ?: start

                    if (start > end) {
                        ranges.add(VerseRange(end, start))
                    } else {
                        ranges.add(VerseRange(start, end))
                    }
                }
            }
            return ranges
        }
    }

    suspend fun parseAndExpand(rawContent: String): List<ScripturePassageBlock> {
        val blocks = mutableListOf<ScripturePassageBlock>()
        if (rawContent.isBlank()) return blocks

        val preCleaned = preClean(rawContent)
        val separated = ConcatenatedReferenceSplitRegex.replace(preCleaned, "\n")
        val lines = separated.split('\n', '\r')

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val refCandidate = stripLeadingItemNumber(trimmed)
            var match = ReferenceRegex.find(refCandidate)
            var isSingleChapterImplicit = false
            
            if (match == null) {
                val scMatch = SingleChapterReferenceRegex.find(refCandidate)
                if (scMatch != null) {
                    val rawBookName = scMatch.groups[1]?.value?.trim() ?: ""
                    val resolvedCanon = resolveCanonicalBookName(rawBookName)
                    if (SingleChapterBooks.contains(resolvedCanon)) {
                        match = scMatch
                        isSingleChapterImplicit = true
                    }
                }
            }

            if (match != null) {
                val rawBookName = match.groups[1]?.value?.trim() ?: ""
                val resolvedCanon = resolveCanonicalBookName(rawBookName)
                val book = bibleService.getBookByName(resolvedCanon) ?: bibleService.getBookByName(rawBookName)
                val bookName = book?.longName ?: resolvedCanon

                val chapterSegments = if (isSingleChapterImplicit) {
                    val versePayload = match.groups[2]?.value ?: ""
                    val ranges = parseVerseRanges(versePayload)
                    if (ranges.isNotEmpty()) listOf(ChapterSegment(1, ranges)) else emptyList()
                } else {
                    parseChapterSegments(match.groups[2]?.value ?: "")
                }

                if (chapterSegments.isNotEmpty()) {
                    var anyAdded = false
                    for (segment in chapterSegments) {
                        val combinedVerses = bibleService.getVerses(bookName, segment.chapter, segment.ranges)

                        if (combinedVerses.isNotEmpty()) {
                            val canonBook = book?.longName ?: combinedVerses[0].bookName ?: bookName
                            val rangeHeader = segment.ranges.joinToString(", ") {
                                if (it.start == it.end) "${it.start}" else "${it.start}-${it.end}"
                            }
                            val header = "$canonBook ${segment.chapter}:$rangeHeader"

                            blocks.add(
                                ScripturePassageBlock(
                                    isScripture = true,
                                    referenceHeader = header,
                                    book = canonBook,
                                    chapter = segment.chapter,
                                    startVerse = segment.ranges.first().start,
                                    endVerse = segment.ranges.last().end,
                                    verses = combinedVerses
                                )
                            )
                            anyAdded = true
                        }
                    }
                    if (anyAdded) continue
                }
            }

            blocks.add(
                ScripturePassageBlock(
                    isScripture = false,
                    plainText = line
                )
            )
        }
        return blocks
    }

    data class ChapterSegment(val chapter: Int, val ranges: List<VerseRange>)
    data class VerseRange(val start: Int, val end: Int)
}
