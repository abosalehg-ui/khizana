package com.abosalehg.khizana.ui.format

import java.util.Locale

/**
 * Counts and page numbers always render with Western digits, per spec.
 *
 * `%d` inside a string resource is formatted with the UI locale, which yields
 * Arabic-Indic digits under some Arabic locales and Western digits under
 * others — so the library header and the reader disagreed about digits on the
 * same device. Formatting here and substituting a `%s` makes it deterministic.
 */
fun formatCount(value: Int): String = String.format(Locale.US, "%d", value)
