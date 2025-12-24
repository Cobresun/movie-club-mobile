package cobresun.movieclub.app.di

import cobresun.movieclub.app.auth.data.network.IdentityDataSource
import cobresun.movieclub.app.auth.data.network.KtorIdentityDataSource
import cobresun.movieclub.app.auth.data.repository.AuthRepositoryImpl
import cobresun.movieclub.app.auth.domain.AuthRepository
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.club.data.network.ClubDataSource
import cobresun.movieclub.app.club.data.network.KtorClubDataSource
import cobresun.movieclub.app.club.presentation.ClubViewModel
import cobresun.movieclub.app.core.data.BearerTokenStorage
import cobresun.movieclub.app.core.data.HttpClientFactory
import cobresun.movieclub.app.member.data.network.KtorMemberDataSource
import cobresun.movieclub.app.member.data.network.MemberDataSource
import cobresun.movieclub.app.member.data.repository.MemberRepositoryImpl
import cobresun.movieclub.app.member.domain.MemberRepository
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.reviews.data.network.KtorReviewsDataSource
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.data.repository.ReviewsRepositoryImpl
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.tmdb.data.network.KtorTmdbDataSource
import cobresun.movieclub.app.tmdb.data.network.TmdbDataSource
import cobresun.movieclub.app.tmdb.data.repository.TmdbRepositoryImpl
import cobresun.movieclub.app.tmdb.domain.TmdbRepository
import cobresun.movieclub.app.watchlist.data.network.KtorWatchListDataSource
import cobresun.movieclub.app.watchlist.data.network.WatchListDataSource
import cobresun.movieclub.app.watchlist.data.repository.WatchListRepositoryImpl
import cobresun.movieclub.app.watchlist.domain.WatchListRepository
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { BearerTokenStorage(get()) }
    single { HttpClientFactory.create(get(), get()) }

    // Auth
    single { KtorIdentityDataSource(get()) }.bind<IdentityDataSource>()
    single { AuthRepositoryImpl(get(), get()) }.bind<AuthRepository>()
    viewModel<AuthViewModel> { AuthViewModel(get()) }

    // Member
    single { KtorMemberDataSource(get()) }.bind<MemberDataSource>()
    single { MemberRepositoryImpl(get()) }.bind<MemberRepository>()
    viewModel { MemberViewModel(get()) }

    // Club
    single { KtorClubDataSource(get()) }.bind<ClubDataSource>()
    viewModelOf(::ClubViewModel)

    // Reviews
    single { KtorReviewsDataSource(get()) }.bind<ReviewsDataSource>()
    single { ReviewsRepositoryImpl(get(), get()) }.bind<ReviewsRepository>()

    // Watchlist
    single { KtorWatchListDataSource(get()) }.bind<WatchListDataSource>()
    single { WatchListRepositoryImpl(get()) }.bind<WatchListRepository>()

    // TMDB
    single { KtorTmdbDataSource(get()) }.bind<TmdbDataSource>()
    single { TmdbRepositoryImpl(get()) }.bind<TmdbRepository>()
}
