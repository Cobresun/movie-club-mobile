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
import cobresun.movieclub.app.club.presentation.ClubSettingsViewModel
import cobresun.movieclub.app.club.presentation.ClubInviteViewModel
import cobresun.movieclub.app.club.presentation.ClubViewModel
import cobresun.movieclub.app.core.data.HttpClientFactory
import cobresun.movieclub.app.core.data.SessionCookieStorage
import cobresun.movieclub.app.core.data.SessionCookieStorageImpl
import cobresun.movieclub.app.core.data.LastClubStoreImpl
import cobresun.movieclub.app.core.domain.LastClubStore
import cobresun.movieclub.app.member.data.network.KtorMemberDataSource
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.data.repository.MemberRepositoryImpl
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.domain.AccountRepository
import cobresun.movieclub.app.member.presentation.CreateClubViewModel
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.member.presentation.JoinClubViewModel
import cobresun.movieclub.app.member.presentation.ProfileViewModel
import cobresun.movieclub.app.lists.data.network.KtorListsDataSource
import cobresun.movieclub.app.lists.data.network.ListsDataSource
import cobresun.movieclub.app.lists.data.repository.ListsRepositoryImpl
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.lists.data.CollapsedListsStoreImpl
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
import cobresun.movieclub.app.lists.presentation.ListsViewModel
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
import org.koin.core.module.Module
import cobresun.movieclub.app.stats.data.StatsRepositoryImpl
import cobresun.movieclub.app.stats.domain.StatsRepository
import cobresun.movieclub.app.stats.presentation.StatsViewModel
import cobresun.movieclub.app.stats.presentation.WrappedViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import cobresun.movieclub.app.providers.presentation.WatchProvidersViewModel
import org.koin.dsl.bind
import org.koin.dsl.binds
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    // Cookie storage
    single { SessionCookieStorageImpl(get()) }.bind<SessionCookieStorage>()

    // HTTP client with cookie support
    single { HttpClientFactory.create(get(), get()) }

    // Auth
    single { KtorBetterAuthDataSource(get()) }.bind<BetterAuthDataSource>()
    single { AuthRepositoryImpl(get(), get()) }.bind<AuthRepository>()
    viewModel<AuthViewModel> { AuthViewModel(get()) }

    // Member
    single { KtorMemberDataSource(get()) }.bind<MemberDataSource>()
    single { MemberRepositoryImpl(get()) } binds arrayOf(MemberRepository::class, AccountRepository::class)
    single { LastClubStoreImpl(get()) }.bind<LastClubStore>()
    single { cobresun.movieclub.app.core.data.WorkDetailsRepositoryImpl(get()) }
        .bind<cobresun.movieclub.app.core.domain.WorkDetailsRepository>()
    viewModel { MemberViewModel(get(), get()) }
    viewModel { CreateClubViewModel(get()) }
    viewModelOf(::JoinClubViewModel)
    viewModelOf(::ProfileViewModel)
    viewModelOf(::ForgotPasswordViewModel)

    // Club
    single { KtorClubDataSource(get()) }.bind<ClubDataSource>()
    single { ClubRepositoryImpl(get()) }.bind<ClubRepository>()
    viewModelOf(::ClubViewModel)
    viewModelOf(::ClubSettingsViewModel)
    viewModelOf(::ClubInviteViewModel)

    // Reviews (one implementation serves scores and comments)
    single { KtorReviewsDataSource(get()) }.bind<ReviewsDataSource>()
    single { ReviewsRepositoryImpl(get()) }.binds(arrayOf(ReviewsRepository::class, ReviewCommentsRepository::class))
    viewModelOf(::ReviewsViewModel)
    viewModelOf(::SharedReviewViewModel)

    // Lists
    single { KtorListsDataSource(get()) }.bind<ListsDataSource>()
    single { ListsRepositoryImpl(get()) }.bind<ListsRepository>()
    single { CollapsedListsStoreImpl(get()) }.bind<CollapsedListsStore>()
    viewModelOf(::ListsViewModel)

    // Awards
    single { KtorAwardsDataSource(get()) }.bind<AwardsDataSource>()
    single { AwardsRepositoryImpl(get()) }.bind<AwardsRepository>()
    viewModelOf(::AwardsViewModel)

    // Search (TMDB / Google Books)
    single { KtorSearchDataSource(get()) }.bind<SearchDataSource>()
    single { WorkSearchRepositoryImpl(get()) }.bind<WorkSearchRepository>()
    viewModelOf(::WorkSearchViewModel)

    single { cobresun.movieclub.app.providers.data.TmdbWatchProvidersRepository(get()) }
        .bind<cobresun.movieclub.app.providers.domain.WatchProvidersRepository>()
    viewModelOf(::WatchProvidersViewModel)

    // Statistics (reuses the reviews and club repositories)
    single { StatsRepositoryImpl(get()) }.bind<StatsRepository>()
    viewModelOf(::StatsViewModel)
    viewModelOf(::WrappedViewModel)
}
