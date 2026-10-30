package sk.odkazprevodica.app.data

import androidx.annotation.StringRes
import sk.odkazprevodica.app.R

/**
 * Preddefinované odkazy. Do databázy sa ukladá iba [id], text sa
 * prekladá na zariadení príjemcu – preto nie je voľný text nikde.
 * Poradie = poradie dlaždíc na obrazovke „Nový odkaz“.
 */
enum class MessageTemplate(
    val id: String,
    @StringRes val shortLabel: Int,
    @StringRes val fullText: Int,
    val iconPath: String,
    val urgent: Boolean = false,
) {
    LIGHTS("lights", R.string.tpl_lights_short, R.string.tpl_lights_full,
        "M9 18h6M10 21h4M12 3a6 6 0 0 0-4 10.5c.7.7 1 1.5 1 2.5h6c0-1 .3-1.8 1-2.5A6 6 0 0 0 12 3z"),
    WINDOW("window", R.string.tpl_window_short, R.string.tpl_window_full,
        "M4 4h16v16H4zM4 12h16M12 4v16"),
    DOOR("door", R.string.tpl_door_short, R.string.tpl_door_full,
        "M6 3h10l2 2v16H6zM14 12h1"),
    TIRE("tire", R.string.tpl_tire_short, R.string.tpl_tire_full,
        "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8z"),
    FAULT("fault", R.string.tpl_fault_short, R.string.tpl_fault_full,
        "M14.7 6.3a4 4 0 0 0-5.4 5.4L3 18l3 3 6.3-6.3a4 4 0 0 0 5.4-5.4l-2.5 2.5-2.4-.6-.6-2.4z"),
    FELL_OFF("fell_off", R.string.tpl_fell_short, R.string.tpl_fell_full,
        "M3 7l9-4 9 4v10l-9 4-9-4zM3 7l9 4 9-4M12 11v10"),
    CHILD_OR_ANIMAL("child_animal", R.string.tpl_child_short, R.string.tpl_child_full,
        "M12 3l10 18H2zM12 10v5M12 18h.01", urgent = true),
    BLOCKING("blocking", R.string.tpl_blocking_short, R.string.tpl_blocking_full,
        "M12 3a9 9 0 1 0 0 18a9 9 0 1 0 0-18zM5.6 5.6l12.8 12.8"),
    TOW("tow", R.string.tpl_tow_short, R.string.tpl_tow_full,
        "M2 16h12V8H2zM14 11h4l3 3v2h-7M6 19a2 2 0 1 0 0-4a2 2 0 1 0 0 4zM17 19a2 2 0 1 0 0-4a2 2 0 1 0 0 4z"),
    DAMAGE("damage", R.string.tpl_damage_short, R.string.tpl_damage_full,
        "M3 12l4-4 3 3 4-6 3 5 4-2M3 18h18"),
    ALARM("alarm", R.string.tpl_alarm_short, R.string.tpl_alarm_full,
        "M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4");

    companion object {
        fun fromId(id: String): MessageTemplate? = entries.firstOrNull { it.id == id }
    }
}

/** Preddefinované odpovede majiteľa. */
enum class ReplyTemplate(
    val id: String,
    @StringRes val label: Int,
    /** Pred odoslaním zobraziť bezpečnostnú výstrahu (majiteľ naznačuje, že pôjde k autu). */
    val needsSafetyWarning: Boolean = false,
) {
    CHECKING("checking", R.string.reply_checking, needsSafetyWarning = true),
    SORRY_CHECKING("sorry_checking", R.string.reply_sorry_checking, needsSafetyWarning = true),
    RESOLVED("resolved", R.string.reply_resolved),
    SORRY_RESOLVED("sorry_resolved", R.string.reply_sorry_resolved);

    companion object {
        fun fromId(id: String?): ReplyTemplate? = entries.firstOrNull { it.id == id }
    }
}
