package cobresun.movieclub.app.di

import cobresun.movieclub.app.auth.data.network.BetterAuthDataSource
import cobresun.movieclub.app.auth.data.network.KtorBetterAuthDataSource
import cobresun.movieclub.app.auth.data.repository.AuthRepositoryImpl
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.auth.presentation.ForgotPasswordViewModel
import cobresun.movieclub.app.awards.data.network.AwardsDataSource
import cobresun.movieclub.app.awards.data.network.KtorAwardsDataSource
import cobresun.movieclub.app.awards.data.repository.AwardsRepositoryImpl
import cobresun.movieclub.app.awards.domain.AwardsRepository
import cobresun.movieclub.app.awards.presentation.AwardsViewModel
import cobresun.movieclub.app.club.data.network.ClubDataSource
import cobresun.movieclub.app.club.data.network.KtorClubDataSource
import cobresun.movieclub.app.club.data.repository.ClubRepositoryImpl
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.presentation.ClubInviteViewModel
import cobresun.movieclub.app.club.presentation.ClubSettingsViewModel
import cobresun.movieclub.app.club.presentation.ClubTab
import cobresun.movieclub.app.club.presentation.ClubViewModel
import cobresun.movieclub.app.core.data.AppConnectivity
import cobresun.movieclub.app.core.data.AppearanceSettingsImpl
import cobresun.movieclub.app.core.data.HttpClientFactory
import cobresun.movieclub.app.core.data.LastClubStoreImpl
import cobresun.movieclub.app.core.data.OfflineStore
import cobresun.movieclub.app.core.data.SessionCaches
import cobresun.movieclub.app.core.data.SessionCookieStorage
import cobresun.movieclub.app.core.data.SessionCookieStorageImpl
import cobresun.movieclub.app.core.data.WorkDetailsRepositoryImpl
import cobresun.movieclub.app.core.domain.AppearanceSettings
import cobresun.movieclub.app.core.domain.Connectivity
import cobresun.movieclub.app.core.domain.LastClubStore
import cobresun.movieclub.app.core.domain.WorkDetailsRepository
import cobresun.movieclub.app.core.presentation.AppearanceViewModel
import cobresun.movieclub.app.core.presentation.ConnectivityViewModel
import cobresun.movieclub.app.lists.data.CollapsedListsStoreImpl
import cobresun.movieclub.app.lists.data.network.KtorListsDataSource
import cobresun.movieclub.app.lists.data.network.ListsDataSource
import cobresun.movieclub.app.lists.data.repository.ListsRepositoryImpl
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.lists.presentation.ListsViewModel
import cobresun.movieclub.app.lists.presentation.SharedListViewModel
import cobresun.movieclub.app.member.data.network.KtorMemberDataSource
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.data.repository.MemberRepositoryImpl
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.presentation.CreateClubViewModel
import cobresun.movieclub.app.member.presentation.JoinClubViewModel
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.member.presentation.ProfileViewModel
import cobresun.movieclub.app.providers.data.TmdbWatchProvidersRepository
import cobresun.movieclub.app.providers.domain.WatchProvidersRepository
import cobresun.movieclub.app.providers.presentation.WatchProvidersViewModel
import cobresun.movieclub.app.reviews.data.ScoreOutbox
import cobresun.movieclub.app.reviews.data.network.KtorReviewsDataSource
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.data.repository.ReviewsRepositoryImpl
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.presentation.ReviewsViewModel
import cobresun.movieclub.app.reviews.presentation.SharedReviewViewModel
import cobresun.movieclub.app.search.data.network.KtorSearchDataSource
import cobresun.movieclub.app.search.data.network.SearchDataSource
import cobresun.movieclub.app.search.data.repository.WorkSearchRepositoryImpl
import cobresun.movieclub.app.search.domain.WorkSearchRepository
import cobresun.movieclub.app.search.presentation.WorkSearchViewModel
import cobresun.movieclub.app.stats.data.StatsRepositoryImpl
import cobresun.movieclub.app.stats.domain.StatsRepository
import cobresun.movieclub.app.stats.presentation.StatsViewModel
import cobresun.movieclub.app.stats.presentation.WrappedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/** Platform implementations: HTTP engine, DataStore, network monitor, offline store, clipboard, haptics. */
expect val platformModule: Module

// Constructor references (`singleOf` / `viewModelOf`) by default; a lambda only where a
// definition needs more than `get()` for each parameter (defaults, callbacks, factories).
// Screens pass their route's arguments with `koinViewModel { parametersOf(...) }`: a lone
// slug resolves by type, so `viewModelOf` still fits; several (or nullable) ones are
// destructured in a lambda, in constructor order.

/** Session plumbing every feature shares: HTTP client, cookies, caches, connectivity. */
val coreModule = module {
    // Session-scoped copies of club reads, shared by the repositories below
    singleOf(::SessionCaches)
    singleOf(::SessionCookieStorageImpl) { bind<SessionCookieStorage>() }
    single { HttpClientFactory.create(get(), get(), get()) }
    singleOf(::LastClubStoreImpl) { bind<LastClubStore>() }
    singleOf(::AppearanceSettingsImpl) { bind<AppearanceSettings>() }
    viewModelOf(::AppearanceViewModel)
    singleOf(::WorkDetailsRepositoryImpl) { bind<WorkDetailsRepository>() }

    // Connectivity: the platform monitor, plus what to do when the network returns
    single<Connectivity> {
        val caches = get<SessionCaches>()
        val reviews = get<ReviewsRepositoryImpl>()
        val offline = get<OfflineStore>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        AppConnectivity(
            monitor = get(),
            onReconnect = {
                caches.invalidateAll()
                offline.forgetStale()
            },
            sendQueued = { reviews.sendQueuedScores() },
            showingSavedData = offline.staleKeys
                .map { it.isNotEmpty() }
                .stateIn(scope, SharingStarted.Eagerly, false),
            scope = scope,
        )
    }
    viewModelOf(::ConnectivityViewModel)
}

val authModule = module {
    singleOf(::KtorBetterAuthDataSource) { bind<BetterAuthDataSource>() }
    singleOf(::AuthRepositoryImpl) { bind<AuthRepository>() }
    viewModelOf(::AuthViewModel)
    viewModelOf(::ForgotPasswordViewModel)
}

val memberModule = module {
    singleOf(::KtorMemberDataSource) { bind<MemberDataSource>() }
    // One implementation serves the member's clubs and their account
    singleOf(::MemberRepositoryImpl) {
        bind<MemberRepository>()
        bind<AccountRepository>()
    }
    viewModelOf(::MemberViewModel)
    viewModelOf(::CreateClubViewModel)
    viewModel { (token: String?) -> JoinClubViewModel(token, get()) }
    viewModelOf(::ProfileViewModel)
}

val clubModule = module {
    singleOf(::KtorClubDataSource) { bind<ClubDataSource>() }
    singleOf(::ClubRepositoryImpl) { bind<ClubRepository>() }
    viewModel { (clubSlug: String, initialTab: ClubTab?) -> ClubViewModel(clubSlug, initialTab, get(), get()) }
    viewModelOf(::ClubSettingsViewModel)
    viewModel { (clubSlug: String, clubName: String, clubType: String) ->
        ClubInviteViewModel(clubSlug, clubName, clubType, get(), get())
    }
}

val reviewsModule = module {
    singleOf(::KtorReviewsDataSource) { bind<ReviewsDataSource>() }
    singleOf(::ScoreOutbox)
    // One implementation serves scores and comments
    singleOf(::ReviewsRepositoryImpl) {
        bind<ReviewsRepository>()
        bind<ReviewCommentsRepository>()
    }
    viewModelOf(::ReviewsViewModel)
    viewModel { (clubSlug: String, workId: String) -> SharedReviewViewModel(clubSlug, workId, get()) }
}

val listsModule = module {
    singleOf(::KtorListsDataSource) { bind<ListsDataSource>() }
    singleOf(::ListsRepositoryImpl) { bind<ListsRepository>() }
    singleOf(::CollapsedListsStoreImpl) { bind<CollapsedListsStore>() }
    viewModelOf(::ListsViewModel)
    viewModel { (clubSlug: String, listId: String) -> SharedListViewModel(clubSlug, listId, get(), get()) }
}

val awardsModule = module {
    singleOf(::KtorAwardsDataSource) { bind<AwardsDataSource>() }
    singleOf(::AwardsRepositoryImpl) { bind<AwardsRepository>() }
    viewModelOf(::AwardsViewModel)
}

/** Search (TMDB / Google Books) and where to stream a work. */
val searchModule = module {
    singleOf(::KtorSearchDataSource) { bind<SearchDataSource>() }
    singleOf(::WorkSearchRepositoryImpl) { bind<WorkSearchRepository>() }
    viewModelOf(::WorkSearchViewModel)

    singleOf(::TmdbWatchProvidersRepository) { bind<WatchProvidersRepository>() }
    viewModelOf(::WatchProvidersViewModel)
}

/** Statistics (reuses the reviews and club repositories). */
val statsModule = module {
    singleOf(::StatsRepositoryImpl) { bind<StatsRepository>() }
    viewModelOf(::StatsViewModel)
    viewModel { (clubSlug: String, year: Int) -> WrappedViewModel(clubSlug, year, get(), get(), get(), get()) }
}

/** Every shared feature module; [initKoin] adds [platformModule] alongside. */
val appModule = module {
    includes(
        coreModule,
        authModule,
        memberModule,
        clubModule,
        reviewsModule,
        listsModule,
        awardsModule,
        searchModule,
        statsModule,
    )
}
