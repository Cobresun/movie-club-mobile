package cobresun.movieclub.app.awards.presentation

import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.NOMINATIONS_PER_MEMBER

/** Copy for each phase of a season (web `AWARDS_PHASES`). */
data class AwardsPhase(
    val label: String,
    /** What members do during the phase, shown under the progress bar. */
    val description: String,
    /** The phase this one closes into, the button that does it, and what closing means. */
    val next: PhaseAdvance? = null,
)

data class PhaseAdvance(val step: AwardsStep, val label: String, val confirm: String)

/**
 * `Completed` is the ceremony after every category has been revealed, so it
 * shares the ceremony's label; revealing the last category is what moves the
 * season there, so no button does.
 */
val AwardsStep.phase: AwardsPhase
    get() = when (this) {
        AwardsStep.CATEGORIES -> AwardsPhase(
            label = "Categories",
            description = "Decide which awards you're handing out. Add your own or pick from the suggestions, then drag them into the order you'll present them.",
            next = PhaseAdvance(AwardsStep.NOMINATIONS, "Open nominations", "Categories will be locked for good."),
        )
        AwardsStep.NOMINATIONS -> AwardsPhase(
            label = "Nominations",
            description = "Everyone nominates at least one (and up to $NOMINATIONS_PER_MEMBER) of the movies the club reviewed this year in every category. Your picks stay private until voting opens, which happens once everyone is done.",
            next = PhaseAdvance(AwardsStep.RANKINGS, "Start voting", "Nominations will close for good."),
        )
        AwardsStep.RANKINGS -> AwardsPhase(
            label = "Voting",
            description = "Put each category's nominees in order, most deserving first, and save. Once everyone has voted, the ceremony can start; the nominee with the best total rank across everyone's ballots wins.",
            next = PhaseAdvance(
                AwardsStep.PRESENTATION,
                "Start the ceremony",
                "Voting will close for good and nobody will be able to change their ballot.",
            ),
        )
        AwardsStep.PRESENTATION -> AwardsPhase(
            label = "Ceremony",
            description = "Get the club together and reveal the winners one category at a time. Once every category is revealed, the awards are complete.",
        )
        AwardsStep.COMPLETED -> AwardsPhase(label = "Ceremony", description = "The winners are in.")
    }

/** "A", "A and B", "A, B and C" (web `listNames`). */
fun listNames(names: List<String>): String =
    if (names.size > 1) "${names.dropLast(1).joinToString(", ")} and ${names.last()}" else names.firstOrNull().orEmpty()
