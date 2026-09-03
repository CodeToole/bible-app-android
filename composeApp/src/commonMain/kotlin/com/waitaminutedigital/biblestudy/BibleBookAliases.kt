package com.waitaminutedigital.biblestudy

object BibleBookAliases {
    private val normalizedAliases = mutableMapOf<String, String>()

    init {
        register("Genesis", "GEN", "GENESIS", "GN")
        register("Exodus", "EX", "EXO", "EXOD", "EXODUS")
        register("Leviticus", "LEV", "LEVIT", "LEVITICUS", "LV")
        register("Numbers", "NUM", "NUMBERS", "NBRS", "NM")
        register("Deuteronomy", "DEUT", "DEUTERONOMY", "DT")
        register("Joshua", "JOSH", "JOSHUA", "JSH")
        register("Judges", "JUDG", "JUDGES", "JDG")
        register("Ruth", "RUTH", "RTH", "RU")
        register("1 Samuel", "1 SAM", "1SAM", "1 SAMUEL", "1SAMUEL", "1 SM", "1SM", "1S")
        register("2 Samuel", "2 SAM", "2SAM", "2 SAMUEL", "2SAMUEL", "2 SM", "2SM", "2S")
        register("1 Kings", "1 KGS", "1KGS", "1 KING", "1KING", "1 KINGS", "1KINGS", "1 KI", "1KI", "1K")
        register("2 Kings", "2 KGS", "2KGS", "2 KING", "2KING", "2 KINGS", "2KINGS", "2 KI", "2KI", "2K")
        register("1 Chronicles", "1 CHRON", "1CHRON", "1 CHRONICLES", "1CHRONICLES", "1 CHR", "1CHR", "1CH")
        register("2 Chronicles", "2 CHRON", "2CHRON", "2 CHRONICLES", "2CHRONICLES", "2 CHR", "2CHR", "2CH")
        register("Ezra", "EZRA", "EZR")
        register("Nehemiah", "NEH", "NEHEMIAH", "NE")
        register("Esther", "ESTH", "ESTHER", "EST", "ES")
        register("Job", "JOB", "JB")
        register("Psalms", "PS", "PSA", "PSALM", "PSALMS", "PSS", "PSM")
        register("Proverbs", "PROV", "PROVERBS", "PRV", "PR")
        register("Ecclesiastes", "ECC", "ECCL", "ECCLESIASTES", "QOH", "KOHELETH", "EC")
        register("Song of Solomon", "SONG", "SONGS", "SONG OF SOLOMON", "SONG OF SONGS", "CANTICLES", "CANTICLE", "SOS")
        register("Isaiah", "ISA", "ISAIAH", "IS")
        register("Jeremiah", "JER", "JEREMIAH", "JR")
        register("Lamentations", "LAM", "LAMENTATIONS", "LM")
        register("Ezekiel", "EZEK", "EZEKIEL", "EZE", "EZ")
        register("Daniel", "DAN", "DANIEL", "DN")
        register("Hosea", "HOS", "HOSEA")
        register("Joel", "JOEL", "JL")
        register("Amos", "AMOS", "AM")
        register("Obadiah", "OBAD", "OBADIAH", "OBA", "OB")
        register("Jonah", "JONAH", "JON")
        register("Micah", "MIC", "MICAH")
        register("Nahum", "NAH", "NAHUM")
        register("Habakkuk", "HAB", "HABAKKUK")
        register("Zephaniah", "ZEPH", "ZEPHANIAH", "ZP")
        register("Haggai", "HAG", "HAGGAI")
        register("Zechariah", "ZECH", "ZECHARIAH", "ZACH", "ZC")
        register("Malachi", "MAL", "MALACHI")
        register("Matthew", "MATT", "MATTHEW", "MAT", "MT")
        register("Mark", "MARK", "MRK", "MK")
        register("Luke", "LUKE", "LUK", "LK")
        register("John", "JOHN", "JHN", "JN")
        register("Acts", "ACTS", "ACT", "AC")
        register("Romans", "ROM", "ROMANS", "RM", "RO")
        register("1 Corinthians", "1 COR", "1COR", "1 CORINTHIANS", "1CORINTHIANS", "1 CO", "1CO", "1C")
        register("2 Corinthians", "2 COR", "2COR", "2 CORINTHIANS", "2CORINTHIANS", "2 CO", "2CO", "2C")
        register("Galatians", "GAL", "GALATIANS", "GA")
        register("Ephesians", "EPH", "EPHESIANS")
        register("Philippians", "PHIL", "PHILIPPIANS", "PHP")
        register("Colossians", "COL", "COLOSSIANS")
        register("1 Thessalonians", "1 THESS", "1THESS", "1 THES", "1THES", "1 THESSALONIANS", "1THESSALONIANS", "1 TH", "1TH", "1TS")
        register("2 Thessalonians", "2 THESS", "2THESS", "2 THES", "2THES", "2 THESSALONIANS", "2THESSALONIANS", "2 TH", "2TH", "2TS")
        register("1 Timothy", "1 TIM", "1TIM", "1 TIMOTHY", "1TIMOTHY", "1 TI", "1TI")
        register("2 Timothy", "2 TIM", "2TIM", "2 TIMOTHY", "2TIMOTHY", "2 TI", "2TI")
        register("Titus", "TIT", "TITUS", "TI")
        register("Philemon", "PHILEM", "PHILEMON", "PHM")
        register("Hebrews", "HEB", "HEBREWS")
        register("James", "JAS", "JAMES", "JM")
        register("1 Peter", "1 PET", "1PET", "1 PETR", "1PETR", "1 PETER", "1PETER", "1 PT", "1PT", "1PE")
        register("2 Peter", "2 PET", "2PET", "2 PETR", "2PETR", "2 PETER", "2PETER", "2 PT", "2PT", "2PE")
        register("1 John", "1 JN", "1JN", "1 JOHN", "1JOHN", "1 JHN", "1JHN", "1J")
        register("2 John", "2 JN", "2JN", "2 JOHN", "2JOHN", "2 JHN", "2JHN", "2J")
        register("3 John", "3 JN", "3JN", "3 JOHN", "3JOHN", "3 JHN", "3JHN", "3J")
        register("Jude", "JUDE", "JUD", "JD", "J")
        register("Revelation", "REV", "REVELATION", "REVELATIONS", "APOCALYPSE", "RV")
    }

    private fun register(canonical: String, vararg variations: String) {
        normalizedAliases[normalizeKey(canonical)] = canonical
        for (v in variations) {
            normalizedAliases[normalizeKey(v)] = canonical
        }
    }

    fun normalizeKey(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch.isLetterOrDigit()) {
                sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    fun getCanonicalName(input: String?): String? {
        if (input.isNullOrBlank()) return null
        val key = normalizeKey(input)
        return normalizedAliases[key]
    }
}
