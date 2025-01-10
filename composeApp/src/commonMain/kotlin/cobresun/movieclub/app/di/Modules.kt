package cobresun.movieclub.app.di

import cobresun.movieclub.app.clubs.data.network.ClubDataSource
import cobresun.movieclub.app.clubs.data.network.KtorClubDataSource
import cobresun.movieclub.app.core.data.HttpClientFactory
import cobresun.movieclub.app.reviews.data.network.KtorReviewsDataSource
import cobresun.movieclub.app.reviews.data.network.ReviewsDataSource
import cobresun.movieclub.app.reviews.data.repository.ReviewsRepositoryImpl
import cobresun.movieclub.app.reviews.domain.ReviewsRepository
import cobresun.movieclub.app.reviews.presentation.ReviewsViewModel
import cobresun.movieclub.app.watchlist.data.network.KtorWatchListDataSource
import cobresun.movieclub.app.watchlist.data.network.WatchListDataSource
import cobresun.movieclub.app.watchlist.data.repository.WatchListRepositoryImpl
import cobresun.movieclub.app.watchlist.domain.WatchListRepository
import cobresun.movieclub.app.watchlist.presentation.WatchListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

expect val platformModule: Module

val sharedModule = module {
    single { HttpClientFactory.create(get()) }

    single { KtorClubDataSource(get()) }.bind<ClubDataSource>()
    single { KtorReviewsDataSource(get()) }.bind<ReviewsDataSource>()
    single { KtorWatchListDataSource(get()) }.bind<WatchListDataSource>()

    single { ReviewsRepositoryImpl(get(), get()) }.bind<ReviewsRepository>()
    single { WatchListRepositoryImpl(get()) }.bind<WatchListRepository>()

    viewModel { ReviewsViewModel(get(), get()) }
    viewModel { WatchListViewModel(get(), get()) }
}
